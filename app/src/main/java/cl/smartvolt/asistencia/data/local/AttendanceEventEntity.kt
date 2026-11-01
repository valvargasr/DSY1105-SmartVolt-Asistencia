package cl.smartvolt.asistencia.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import cl.smartvolt.asistencia.native.location.MockSignal

/**
 * Entidad Room que representa una marca de asistencia persistida en SQLite.
 *
 * Reemplaza la implementación anterior basada en SharedPreferences y garantiza
 * que las marcas sobrevivan al cierre de la aplicación (requisito P0-05).
 *
 * [syncState] nace en [PENDING_SYNC] y no se presenta como validada por el
 * servidor hasta que exista respuesta del backend (RNF-04).
 */
@Entity(tableName = "attendance_events")
data class AttendanceEventEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val timestampMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val beaconId: String?,
    val rssi: Int?,
    val mockSignal: String,
    val workerId: String,
    val siteId: String,
    val syncState: String
) {
    companion object {
        const val TYPE_CHECK_IN = "CHECK_IN"
        const val TYPE_CHECK_OUT = "CHECK_OUT"
        const val PENDING_SYNC = "PENDING_SYNC"
        const val SYNCED = "SYNCED"
        const val REJECTED = "REJECTED"
        const val REVIEW = "REVIEW"

        const val WORKER_ID = "OP-001"
        const val SITE_ID = "FAENA-DEMO-01"
    }
}

/** Conversión entre el modelo de dominio y la entidad Room. */
fun LocalAttendanceEvent.toEntity(): AttendanceEventEntity = AttendanceEventEntity(
    id = id,
    type = type,
    timestampMillis = timestampMillis,
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
    beaconId = beaconId,
    rssi = rssi,
    mockSignal = mockSignal.name,
    workerId = workerId,
    siteId = siteId,
    syncState = syncState
)

fun AttendanceEventEntity.toDomain(): LocalAttendanceEvent = LocalAttendanceEvent(
    id = id,
    type = type,
    timestampMillis = timestampMillis,
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
    beaconId = beaconId,
    rssi = rssi,
    mockSignal = runCatching { MockSignal.valueOf(mockSignal) }.getOrDefault(MockSignal.UNKNOWN),
    workerId = workerId,
    siteId = siteId,
    syncState = syncState
)
