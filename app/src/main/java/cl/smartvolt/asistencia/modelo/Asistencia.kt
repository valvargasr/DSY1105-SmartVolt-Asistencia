package cl.smartvolt.asistencia.modelo

import androidx.room.Entity
import androidx.room.PrimaryKey

// una marca es una entrada o una salida, no las dos juntas
@Entity(tableName = "asistencias")
data class Asistencia(
    @PrimaryKey
    val id: String,
    val workerId: String,
    val siteId: String,
    val timestampMillis: Long,
    val type: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val beaconId: String? = null,
    val rssi: Int? = null,
    val mockSignal: Boolean = false,
    val syncState: String = PENDING_SYNC
) {
    val esEntrada: Boolean
        get() = type == TYPE_CHECK_IN

    companion object {
        const val TYPE_CHECK_IN = "CHECK_IN"
        const val TYPE_CHECK_OUT = "CHECK_OUT"
        const val PENDING_SYNC = "PENDING_SYNC"
        const val WORKER_ID = "OP-001"
        const val SITE_ID = "FAENA-DEMO-01"
    }
}
