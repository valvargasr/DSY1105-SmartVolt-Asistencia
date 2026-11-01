package cl.smartvolt.asistencia.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import cl.smartvolt.asistencia.domain.model.OvertimeApproval
import cl.smartvolt.asistencia.domain.model.OvertimeStatus
import cl.smartvolt.asistencia.domain.model.WorkerRole
import java.time.ZoneOffset

/**
 * Entidad Room para autorizaciones de horas extra.
 *
 * Persiste las solicitudes y decisiones administrativas para que sobrevivan
 * al cierre de la aplicación (RNF-05). No incluye montos ni remuneraciones.
 */
@Entity(tableName = "overtime_approvals")
data class OvertimeApprovalEntity(
    @PrimaryKey
    val approvalId: String,
    val workerId: String,
    val siteId: String,
    val dateMillis: Long,
    val authorizedStartMillis: Long,
    val authorizedEndMillis: Long,
    val approverRole: String,
    val approverId: String,
    val status: String,
    val reason: String?,
    val createdAtMillis: Long
)

/** Conversión de modelo de dominio a entidad Room. */
fun OvertimeApproval.toEntity(): OvertimeApprovalEntity = OvertimeApprovalEntity(
    approvalId = approvalId,
    workerId = workerId,
    siteId = siteId,
    dateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    authorizedStartMillis = authorizedStartMillis,
    authorizedEndMillis = authorizedEndMillis,
    approverRole = approverRole.name,
    approverId = approverId,
    status = status.name,
    reason = reason,
    createdAtMillis = createdAtMillis
)

/** Conversión de entidad Room a modelo de dominio. */
fun OvertimeApprovalEntity.toDomain(): OvertimeApproval = OvertimeApproval(
    approvalId = approvalId,
    workerId = workerId,
    siteId = siteId,
    date = java.time.Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate(),
    authorizedStartMillis = authorizedStartMillis,
    authorizedEndMillis = authorizedEndMillis,
    approverRole = runCatching { WorkerRole.valueOf(approverRole) }.getOrDefault(WorkerRole.ADMIN),
    approverId = approverId,
    status = runCatching { OvertimeStatus.valueOf(status) }.getOrDefault(OvertimeStatus.PENDING),
    reason = reason,
    createdAtMillis = createdAtMillis
)
