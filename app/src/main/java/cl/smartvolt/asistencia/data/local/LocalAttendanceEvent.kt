package cl.smartvolt.asistencia.data.local

import cl.smartvolt.asistencia.native.location.MockSignal

/**
 * Marca de asistencia guardada en el teléfono.
 *
 * Mantiene únicamente los datos necesarios para reconstruir la evidencia y
 * sincronizarla después. [syncState] nace en PENDING_SYNC y no se presenta
 * como validada por el servidor hasta que exista respuesta del backend.
 */
data class LocalAttendanceEvent(
    val id: String,
    val type: String,
    val timestampMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val beaconId: String?,
    val rssi: Int?,
    val mockSignal: MockSignal = MockSignal.UNKNOWN,
    val workerId: String = WORKER_ID,
    val siteId: String = SITE_ID,
    val syncState: String = PENDING_SYNC
) {
    companion object {
        const val TYPE_CHECK_IN = "CHECK_IN"
        const val TYPE_CHECK_OUT = "CHECK_OUT"
        const val PENDING_SYNC = "PENDING_SYNC"

        /** Identidad ficticia de demostración. No se usan datos de personas reales. */
        const val WORKER_ID = "OP-001"

        /** Faena ficticia de demostración. */
        const val SITE_ID = "FAENA-DEMO-01"
    }
}
