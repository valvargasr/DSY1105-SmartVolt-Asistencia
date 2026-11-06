package cl.smartvolt.asistencia.modelo

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// cada solicitud queda con su estado hasta que el admin la revise
@Entity(tableName = "solicitudes_horas_extra")
data class SolicitudHoraExtra(
    @PrimaryKey
    val approvalId: String,
    val workerId: String,
    val siteId: String,
    val dateMillis: Long,
    val authorizedStartMillis: Long,
    val authorizedEndMillis: Long,
    val approverRole: String,
    val approverId: String,
    val status: String = PENDIENTE,
    val reason: String? = null,
    val decisionNote: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val fecha: LocalDate
        get() = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()

    val estado: EstadoSolicitud
        get() = EstadoSolicitud.entries.firstOrNull { it.name == status }
            ?: EstadoSolicitud.PENDIENTE

    companion object {
        const val PENDIENTE = "PENDIENTE"
        const val APROBADA = "APROBADA"
        const val RECHAZADA = "RECHAZADA"
        const val CANCELADA = "CANCELADA"
    }
}

enum class EstadoSolicitud(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada"),
    CANCELADA("Cancelada")
}
