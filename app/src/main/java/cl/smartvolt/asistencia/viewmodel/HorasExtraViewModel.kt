package cl.smartvolt.asistencia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.modelo.Asistencia
import cl.smartvolt.asistencia.modelo.SolicitudHoraExtra
import cl.smartvolt.asistencia.modelo.WorkerRole
import cl.smartvolt.asistencia.util.Sesion
import cl.smartvolt.asistencia.util.Validaciones
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Errores y mensajes del formulario de horas extra
data class HorasExtraUiState(
    val errores: Validaciones.ErroresHoraExtra = Validaciones.ErroresHoraExtra(),
    val mensaje: String? = null,
    val guardando: Boolean = false
)

// Crea, aprueba y rechaza solicitudes de horas extra
class HorasExtraViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.obtener(application).solicitudHoraExtraDao()
    private val cuenta = Sesion.cuenta

    private val workerId = cuenta?.workerId ?: Asistencia.WORKER_ID
    private val siteId = cuenta?.siteId ?: Asistencia.SITE_ID

    private val _uiState = MutableStateFlow(HorasExtraUiState())
    val uiState: StateFlow<HorasExtraUiState> = _uiState.asStateFlow()

    // El admin ve toda la faena, el obrero solo lo suyo
    val solicitudes: StateFlow<List<SolicitudHoraExtra>> =
        if (cuenta?.role == WorkerRole.ADMIN) {
            dao.observarPorFaena(siteId)
        } else {
            dao.observarPorTrabajador(workerId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Valida y guarda una solicitud nueva
    fun solicitar(fecha: LocalDate?, desde: String, hasta: String, motivo: String) {
        // el obrero es el único que puede crear solicitudes
        if (cuenta?.role != WorkerRole.WORKER) {
            _uiState.update { it.copy(mensaje = "Solo el obrero puede crear una solicitud.") }
            return
        }

        when (val resultado = Validaciones.validarHoraExtra(fecha, desde, hasta, motivo)) {
            is Validaciones.ResultadoHoraExtra.Invalida ->
                _uiState.update { it.copy(errores = resultado.errores, mensaje = null) }

            is Validaciones.ResultadoHoraExtra.Valida -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(guardando = true, mensaje = null) }
                    try {
                        val datos = resultado.datos
                        dao.guardar(
                            SolicitudHoraExtra(
                                approvalId = "OT-${UUID.randomUUID()}",
                                workerId = workerId,
                                siteId = siteId,
                                dateMillis = datos.date.atStartOfDay(java.time.ZoneOffset.UTC)
                                    .toInstant().toEpochMilli(),
                                authorizedStartMillis = datos.startMillis,
                                authorizedEndMillis = datos.endMillis,
                                approverRole = WorkerRole.ADMIN.name,
                                approverId = "",
                                status = SolicitudHoraExtra.PENDIENTE,
                                reason = datos.reason
                            )
                        )
                        _uiState.update {
                            it.copy(
                                guardando = false,
                                errores = Validaciones.ErroresHoraExtra(),
                                mensaje = "Solicitud enviada. Puedes revisar su estado aquí."
                            )
                        }
                    } catch (error: Exception) {
                        _uiState.update {
                            it.copy(
                                guardando = false,
                                mensaje = "No se pudo guardar la solicitud. Intenta de nuevo."
                            )
                        }
                    }
                }
            }
        }
    }

    // Aprueba una solicitud pendiente
    fun aprobar(approvalId: String) {
        if (cuenta?.role != WorkerRole.ADMIN) return
        decidir(approvalId, SolicitudHoraExtra.APROBADA, null, "Solicitud aprobada.")
    }

    // Rechaza una solicitud con un motivo
    fun rechazar(approvalId: String, motivo: String) {
        if (cuenta?.role != WorkerRole.ADMIN || motivo.isBlank()) return
        decidir(approvalId, SolicitudHoraExtra.RECHAZADA, motivo.trim(), "Solicitud rechazada.")
    }

    private fun decidir(
        approvalId: String,
        estado: String,
        nota: String?,
        mensaje: String
    ) {
        // aprobar y rechazar terminan acá, solo cambia el estado
        viewModelScope.launch {
            try {
                dao.decidir(approvalId, estado, cuenta?.workerId.orEmpty(), nota)
                _uiState.update { it.copy(mensaje = mensaje) }
            } catch (error: Exception) {
                _uiState.update { it.copy(mensaje = "No se pudo actualizar la solicitud.") }
            }
        }
    }

    // Limpia el mensaje para no repetirlo
    fun limpiarMensaje() {
        _uiState.update { it.copy(mensaje = null) }
    }
}
