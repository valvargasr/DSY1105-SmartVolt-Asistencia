package cl.smartvolt.asistencia.domain.model

import java.time.LocalDate

/**
 * Autorización de horas extra (sin cálculo de remuneración).
 *
 * La autorización no demuestra que las horas se trabajaron: solo el estado
 * administrativo de la autorización.
 */
data class OvertimeApproval(
    val approvalId: String,
    val workerId: String,
    val siteId: String,
    val date: LocalDate,
    val authorizedStartMillis: Long,
    val authorizedEndMillis: Long,
    val approverRole: WorkerRole,
    val approverId: String,
    val status: OvertimeStatus = OvertimeStatus.PENDING,
    val reason: String? = null,
    /** Nota del administrador al aprobar o rechazar; no reemplaza el motivo. */
    val decisionNote: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
