package cl.smartvolt.asistencia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.modelo.Asistencia
import cl.smartvolt.asistencia.modelo.Faena
import cl.smartvolt.asistencia.modelo.faenaPorDefecto
import cl.smartvolt.asistencia.util.Baliza
import cl.smartvolt.asistencia.util.EstadoBaliza
import cl.smartvolt.asistencia.util.EstadoPermiso
import cl.smartvolt.asistencia.util.EstadoUbicacion
import cl.smartvolt.asistencia.util.Fechas
import cl.smartvolt.asistencia.util.Geocerca
import cl.smartvolt.asistencia.util.Jornada
import cl.smartvolt.asistencia.util.Notificacion
import cl.smartvolt.asistencia.util.ResultadoBaliza
import cl.smartvolt.asistencia.util.ResultadoUbicacion
import cl.smartvolt.asistencia.util.Sesion
import cl.smartvolt.asistencia.util.Ubicacion
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// todo lo que la pantalla necesita saber para dejar marcar
data class AsistenciaUiState(
    val permiso: EstadoPermiso = EstadoPermiso.NO_SOLICITADO,
    val ubicacion: ResultadoUbicacion? = null,
    val baliza: ResultadoBaliza? = null,
    val faena: Faena = faenaPorDefecto(),
    val distanciaMetros: Double? = null,
    val verificando: Boolean = false,
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val pendientes: Int = 0,
    val jornada: Jornada = Jornada(),
    val ultimaMarca: Asistencia? = null
) {
    val dentroDeFaena: Boolean
        get() = distanciaMetros == null || distanciaMetros <= faena.radiusMeters

    val puedeRegistrar: Boolean
        get() = ubicacion?.estado == EstadoUbicacion.LISTA &&
            baliza?.estado == EstadoBaliza.ENCONTRADA &&
            dentroDeFaena

    val puedeEntrar: Boolean get() = puedeRegistrar && !jornada.tieneEntrada
    val puedeSalir: Boolean get() = puedeRegistrar && jornada.entradaAbierta

    val mensajeUbicacion: String
        get() {
            val resultado = ubicacion ?: return "Aún no se ha verificado la ubicación."
            return when (resultado.estado) {
                EstadoUbicacion.LISTA -> {
                    val precision = resultado.accuracyMeters
                    val base = if (precision != null) {
                        "Ubicación lista. Precisión estimada: ${precision.roundToInt()} m."
                    } else {
                        "Ubicación lista."
                    }
                    val distancia = distanciaMetros
                    if (distancia == null) {
                        "$base Sin referencia de la faena."
                    } else if (distancia <= faena.radiusMeters) {
                        "$base Estás dentro de ${faena.name} (${distancia.roundToInt()} m del centro)."
                    } else {
                        "$base Estás fuera de ${faena.name} (${distancia.roundToInt()} m del centro, " +
                            "radio ${faena.radiusMeters} m)."
                    }
                }
                EstadoUbicacion.SIN_PERMISO -> "Falta el permiso de ubicación."
                EstadoUbicacion.UBICACION_APAGADA -> "La ubicación del teléfono está apagada."
                EstadoUbicacion.TIEMPO_AGOTADO -> "No se obtuvo una lectura de GPS a tiempo."
                EstadoUbicacion.NO_DISPONIBLE -> "La ubicación no está disponible en este equipo."
                EstadoUbicacion.ERROR -> "Ocurrió un error al leer la ubicación."
            }
        }

    val mensajeBaliza: String
        get() {
            val resultado = baliza ?: return "Aún no se ha escaneado la baliza."
            return when (resultado.estado) {
                EstadoBaliza.ENCONTRADA -> "Baliza detectada con señal ${resultado.rssi ?: 0} dBm."
                EstadoBaliza.NO_ENCONTRADA -> "No se detectó la baliza. Acércate e intenta de nuevo."
                EstadoBaliza.SIN_PERMISO -> "Falta el permiso de Bluetooth para escanear."
                EstadoBaliza.BLUETOOTH_APAGADO -> "El Bluetooth está apagado."
                EstadoBaliza.NO_SOPORTADA -> "Este teléfono no soporta Bluetooth de baja energía."
                EstadoBaliza.TIEMPO_AGOTADO -> "El escaneo de la baliza no terminó a tiempo."
                EstadoBaliza.ERROR -> "Ocurrió un error al escanear la baliza."
            }
        }
}

class AsistenciaViewModel(application: Application) : AndroidViewModel(application) {

    private val asistenciaDao = AppDatabase.obtener(application).asistenciaDao()
    private val faenaDao = AppDatabase.obtener(application).faenaDao()

    private val ubicacion = Ubicacion(application)
    private val baliza = Baliza(application)
    private val notificacion = Notificacion(application)

    private val workerId = Sesion.cuenta?.workerId ?: Asistencia.WORKER_ID
    private val siteId = Sesion.cuenta?.siteId ?: Asistencia.SITE_ID

    private val _uiState = MutableStateFlow(AsistenciaUiState())
    val uiState: StateFlow<AsistenciaUiState> = _uiState.asStateFlow()

    init {
        // escuchamos las marcas y la faena activa para recalcular la jornada solo
        viewModelScope.launch {
            combine(
                asistenciaDao.observarPorTrabajador(workerId),
                asistenciaDao.observarPendientes(),
                faenaDao.observarActiva()
            ) { marcas, pendientes, faenaGuardada ->
                Triple(marcas, pendientes, faenaGuardada ?: faenaPorDefecto())
            }.collect { (marcas, pendientes, faena) ->
                _uiState.update {
                    it.copy(
                        faena = faena,
                        pendientes = pendientes.size,
                        jornada = Fechas.estadoJornada(marcas)
                    )
                }
            }
        }
    }

    fun actualizarPermiso(concedido: Boolean, bloqueado: Boolean) {
        _uiState.update {
            it.copy(
                permiso = when {
                    concedido -> EstadoPermiso.CONCEDIDO
                    bloqueado -> EstadoPermiso.BLOQUEADO
                    else -> EstadoPermiso.DENEGADO
                }
            )
        }
    }

    fun verificar() {
        if (_uiState.value.verificando) return

        // ubicación y baliza se piden en paralelo para no demorar el doble
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    verificando = true,
                    mensaje = null,
                    ubicacion = null,
                    baliza = null,
                    distanciaMetros = null
                )
            }

            try {
                val lecturaPendiente = async { ubicacion.obtener() }
                val escaneoPendiente = async { baliza.escanear() }
                val lectura = lecturaPendiente.await()
                val escaneo = escaneoPendiente.await()
                val faena = faenaDao.obtener(siteId) ?: faenaPorDefecto()
                val distancia = if (lectura.estado == EstadoUbicacion.LISTA &&
                    lectura.latitude != null && lectura.longitude != null
                ) {
                    Geocerca.distanciaMetros(
                        lectura.latitude,
                        lectura.longitude,
                        faena.latitude,
                        faena.longitude
                    )
                } else {
                    null
                }

                _uiState.update {
                    it.copy(
                        ubicacion = lectura,
                        baliza = escaneo,
                        faena = faena,
                        distanciaMetros = distancia,
                        permiso = when (lectura.estado) {
                            EstadoUbicacion.LISTA -> EstadoPermiso.CONCEDIDO
                            EstadoUbicacion.SIN_PERMISO -> EstadoPermiso.DENEGADO
                            else -> it.permiso
                        }
                    )
                }
            } catch (cancelado: CancellationException) {
                throw cancelado
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(mensaje = "No se pudo verificar la presencia. Intenta de nuevo.")
                }
            } finally {
                _uiState.update { it.copy(verificando = false) }
            }
        }
    }

    fun registrarEntrada() = registrar(Asistencia.TYPE_CHECK_IN)

    fun registrarSalida() = registrar(Asistencia.TYPE_CHECK_OUT)

    private fun registrar(tipo: String) {
        val estado = _uiState.value
        if (estado.guardando) return

        // ojo, la entrada y la salida se guardan como marcas separadas
        if (tipo == Asistencia.TYPE_CHECK_IN && estado.jornada.tieneEntrada) {
            _uiState.update { it.copy(mensaje = "Ya registraste tu entrada hoy.") }
            return
        }
        if (tipo == Asistencia.TYPE_CHECK_OUT && !estado.jornada.entradaAbierta) {
            _uiState.update {
                it.copy(mensaje = "No hay una entrada abierta para registrar la salida.")
            }
            return
        }
        if (!estado.puedeRegistrar) {
            _uiState.update { it.copy(mensaje = motivoParaNoRegistrar(estado)) }
            return
        }

        val lectura = estado.ubicacion ?: return
        val escaneo = estado.baliza ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, mensaje = null) }

            try {
                val marca = Asistencia(
                    id = UUID.randomUUID().toString(),
                    workerId = workerId,
                    siteId = siteId,
                    timestampMillis = lectura.capturadaMillis ?: System.currentTimeMillis(),
                    type = tipo,
                    latitude = lectura.latitude,
                    longitude = lectura.longitude,
                    accuracyMeters = lectura.accuracyMeters,
                    beaconId = escaneo.beaconId,
                    rssi = escaneo.rssi,
                    mockSignal = lectura.simulada,
                    syncState = Asistencia.PENDING_SYNC
                )
                asistenciaDao.insertar(marca)

                if (tipo == Asistencia.TYPE_CHECK_IN) {
                    notificacion.avisarEntrada()
                } else {
                    notificacion.avisarSalida()
                }

                val nombre = if (tipo == Asistencia.TYPE_CHECK_IN) "Entrada" else "Salida"
                _uiState.update {
                    it.copy(
                        ultimaMarca = marca,
                        mensaje = "$nombre registrada a las ${Fechas.hora(marca.timestampMillis)}. " +
                            "Guardada en este dispositivo y pendiente de sincronización."
                    )
                }
            } catch (cancelado: CancellationException) {
                throw cancelado
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(mensaje = "No se pudo guardar la marca. Intenta de nuevo.")
                }
            } finally {
                _uiState.update { it.copy(guardando = false) }
            }
        }
    }

    private fun motivoParaNoRegistrar(estado: AsistenciaUiState): String {
        if (estado.ubicacion?.estado != EstadoUbicacion.LISTA) {
            return "Primero verifica la presencia: se necesita una lectura de ubicación."
        }
        if (estado.baliza?.estado != EstadoBaliza.ENCONTRADA) {
            return "Primero verifica la presencia: se necesita detectar la baliza."
        }
        val distancia = estado.distanciaMetros
        if (distancia != null && distancia > estado.faena.radiusMeters) {
            return "No puedes registrar: estás a ${distancia.roundToInt()} m del centro de " +
                "${estado.faena.name}. El radio permitido es de ${estado.faena.radiusMeters} m."
        }
        return "No se puede registrar la marca en este momento."
    }

    fun descartarMensaje() {
        _uiState.update { it.copy(mensaje = null) }
    }
}
