package cl.smartvolt.asistencia.feature.overtime

import androidx.lifecycle.ViewModel
import cl.smartvolt.asistencia.domain.model.OvertimeApproval
import cl.smartvolt.asistencia.domain.model.OvertimeStatus
import cl.smartvolt.asistencia.domain.model.WorkerRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * ViewModel para la gestión de solicitudes de horas extra.
 *
 * El trabajador solicita; el administrador aprueba o rechaza.
 * No se calculan remuneraciones ni liquidaciones (RB-09).
 */
class OvertimeViewModel : ViewModel() {

    private val _requests = MutableStateFlow<List<OvertimeApproval>>(emptyList())
    val requests: StateFlow<List<OvertimeApproval>> = _requests.asStateFlow()

    /**
     * El trabajador envía una solicitud de horas extra.
     */
    fun solicitar(
        workerId: String,
        siteId: String,
        date: LocalDate,
        startMillis: Long,
        endMillis: Long,
        reason: String?
    ) {
        val approval = OvertimeApproval(
            approvalId = "OT-${System.currentTimeMillis()}",
            workerId = workerId,
            siteId = siteId,
            date = date,
            authorizedStartMillis = startMillis,
            authorizedEndMillis = endMillis,
            approverRole = WorkerRole.ADMIN,
            approverId = "",
            status = OvertimeStatus.PENDING,
            reason = reason
        )
        _requests.value = _requests.value + approval
    }

    /**
     * El administrador aprueba una solicitud.
     */
    fun aprobar(approvalId: String, approverId: String) {
        _requests.value = _requests.value.map { req ->
            if (req.approvalId == approvalId && req.status == OvertimeStatus.PENDING) {
                req.copy(
                    status = OvertimeStatus.APPROVED,
                    approverId = approverId
                )
            } else {
                req
            }
        }
    }

    /**
     * El administrador rechaza una solicitud (requiere motivo).
     */
    fun rechazar(approvalId: String, approverId: String, motivo: String) {
        if (motivo.isBlank()) return
        _requests.value = _requests.value.map { req ->
            if (req.approvalId == approvalId && req.status == OvertimeStatus.PENDING) {
                req.copy(
                    status = OvertimeStatus.REJECTED,
                    approverId = approverId,
                    reason = motivo
                )
            } else {
                req
            }
        }
    }

    /**
     * Cancela una solicitud pendiente.
     */
    fun cancelar(approvalId: String) {
        _requests.value = _requests.value.map { req ->
            if (req.approvalId == approvalId && req.status == OvertimeStatus.PENDING) {
                req.copy(status = OvertimeStatus.CANCELLED)
            } else {
                req
            }
        }
    }

    fun porTrabajador(workerId: String): List<OvertimeApproval> =
        _requests.value.filter { it.workerId == workerId }

    fun pendientes(): List<OvertimeApproval> =
        _requests.value.filter { it.status == OvertimeStatus.PENDING }
}
