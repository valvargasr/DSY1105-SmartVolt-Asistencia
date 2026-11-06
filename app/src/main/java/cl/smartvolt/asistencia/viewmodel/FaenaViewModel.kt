package cl.smartvolt.asistencia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.modelo.Asistencia
import cl.smartvolt.asistencia.modelo.faenaPorDefecto
import cl.smartvolt.asistencia.util.Sesion
import cl.smartvolt.asistencia.util.Ubicacion
import cl.smartvolt.asistencia.util.Validaciones
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FaenaUiState(
    val nombre: String = "",
    val direccion: String = "",
    val radio: String = "",
    val latitud: String = "",
    val longitud: String = "",
    val tarifa: String = "",
    val recargo: String = "",
    val errores: Validaciones.ErroresFaena = Validaciones.ErroresFaena(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val ubicacionEnCurso: Boolean = false,
    val mensaje: String? = null,
    val guardado: Boolean = false
) {
    val latitudPreview: Double? get() = latitud.replace(',', '.').toDoubleOrNull()
    val longitudPreview: Double? get() = longitud.replace(',', '.').toDoubleOrNull()
}

class FaenaViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.obtener(application).faenaDao()
    private val ubicacion = Ubicacion(application)
    private val siteId = Sesion.cuenta?.siteId ?: Asistencia.SITE_ID

    private val _uiState = MutableStateFlow(FaenaUiState())
    val uiState: StateFlow<FaenaUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            val faena = dao.obtener(siteId) ?: faenaPorDefecto()
            _uiState.update {
                it.copy(
                    nombre = faena.name,
                    direccion = faena.address,
                    radio = faena.radiusMeters.toString(),
                    latitud = formato(faena.latitude),
                    longitud = formato(faena.longitude),
                    tarifa = faena.hourlyRateClp.toString(),
                    recargo = faena.overtimeSurchargePercent.toString(),
                    cargando = false
                )
            }
        }
    }

    fun onNombre(valor: String) = actualizar { copy(nombre = valor, errores = errores.copy(name = "")) }

    fun onDireccion(valor: String) =
        actualizar { copy(direccion = valor, errores = errores.copy(address = "")) }

    fun onRadio(valor: String) {
        if (!valor.all { it.isDigit() }) return
        actualizar { copy(radio = valor, errores = errores.copy(radiusMeters = "")) }
    }

    fun onLatitud(valor: String) =
        actualizar { copy(latitud = valor, errores = errores.copy(latitude = "")) }

    fun onLongitud(valor: String) =
        actualizar { copy(longitud = valor, errores = errores.copy(longitude = "")) }

    fun onTarifa(valor: String) {
        if (!valor.all { it.isDigit() }) return
        actualizar { copy(tarifa = valor, errores = errores.copy(hourlyRateClp = "")) }
    }

    fun onRecargo(valor: String) {
        if (!valor.all { it.isDigit() }) return
        actualizar { copy(recargo = valor, errores = errores.copy(overtimeSurchargePercent = "")) }
    }

    fun usarUbicacionActual() {
        if (_uiState.value.ubicacionEnCurso) return

        viewModelScope.launch {
            _uiState.update { it.copy(ubicacionEnCurso = true, mensaje = null) }
            try {
                val lectura = ubicacion.obtener()
                if (lectura.latitude != null && lectura.longitude != null &&
                    lectura.estado == cl.smartvolt.asistencia.util.EstadoUbicacion.LISTA
                ) {
                    _uiState.update {
                        it.copy(
                            latitud = formato(lectura.latitude),
                            longitud = formato(lectura.longitude),
                            errores = it.errores.copy(latitude = "", longitude = ""),
                            mensaje = "Se usó la ubicación actual del teléfono."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(mensaje = "No se pudo leer la ubicación. Revisa permisos y GPS.")
                    }
                }
            } catch (cancelado: CancellationException) {
                throw cancelado
            } catch (error: Exception) {
                _uiState.update { it.copy(mensaje = "No se pudo leer la ubicación.") }
            } finally {
                _uiState.update { it.copy(ubicacionEnCurso = false) }
            }
        }
    }

    fun guardar() {
        val estado = _uiState.value
        if (estado.guardando) return

        when (val resultado = Validaciones.validarFaena(
            name = estado.nombre,
            address = estado.direccion,
            radiusMeters = estado.radio,
            latitude = estado.latitud,
            longitude = estado.longitud,
            hourlyRateClp = estado.tarifa,
            overtimeSurchargePercent = estado.recargo
        )) {
            is Validaciones.ResultadoFaena.Invalida ->
                _uiState.update { it.copy(errores = resultado.errores, mensaje = null) }

            is Validaciones.ResultadoFaena.Valida -> {
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(guardando = true, errores = Validaciones.ErroresFaena(), mensaje = null)
                    }
                    try {
                        val anterior = dao.obtener(siteId) ?: faenaPorDefecto()
                        val datos = resultado.datos
                        dao.guardar(
                            anterior.copy(
                                name = datos.name,
                                address = datos.address,
                                latitude = datos.latitude,
                                longitude = datos.longitude,
                                radiusMeters = datos.radiusMeters,
                                hourlyRateClp = datos.hourlyRateClp,
                                overtimeSurchargePercent = datos.overtimeSurchargePercent,
                                perimeterVersion = anterior.perimeterVersion + 1
                            )
                        )
                        _uiState.update {
                            it.copy(
                                guardando = false,
                                guardado = true,
                                mensaje = "Faena actualizada."
                            )
                        }
                    } catch (cancelado: CancellationException) {
                        throw cancelado
                    } catch (error: Exception) {
                        _uiState.update {
                            it.copy(
                                guardando = false,
                                mensaje = "No se pudo guardar la faena. Intenta de nuevo."
                            )
                        }
                    }
                }
            }
        }
    }

    private fun actualizar(cambio: FaenaUiState.() -> FaenaUiState) {
        _uiState.update { it.cambio().copy(mensaje = null) }
    }

    private fun formato(valor: Double): String = String.format(Locale.US, "%.6f", valor)
}
