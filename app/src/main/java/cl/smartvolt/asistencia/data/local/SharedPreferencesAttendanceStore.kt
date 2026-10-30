package cl.smartvolt.asistencia.data.local

import android.content.Context
import cl.smartvolt.asistencia.native.location.MockSignal
import org.json.JSONArray
import org.json.JSONObject

/**
 * Implementación de [AttendanceLocalStore] sobre SharedPreferences + JSON.
 *
 * Es deliberadamente simple: el objetivo del hito es demostrar que la marca
 * sobrevive al cierre de la app. Si el JSON guardado está corrupto se descarta
 * en lugar de romper la pantalla.
 */
class SharedPreferencesAttendanceStore(
    context: Context
) : AttendanceLocalStore {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun save(event: LocalAttendanceEvent) {
        val events = readEvents().toMutableList()
        events.add(event)
        writeEvents(events)
    }

    @Synchronized
    override fun pending(): List<LocalAttendanceEvent> =
        readEvents().filter { it.syncState == LocalAttendanceEvent.PENDING_SYNC }

    @Synchronized
    override fun count(): Int = pending().size

    private fun readEvents(): List<LocalAttendanceEvent> {
        val raw = preferences.getString(KEY_EVENTS, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()

        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            runCatching { item.toEvent() }.getOrNull()
        }
    }

    private fun writeEvents(events: List<LocalAttendanceEvent>) {
        val array = JSONArray()
        events.forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    private fun LocalAttendanceEvent.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type)
        put("timestampMillis", timestampMillis)
        put("latitude", latitude)
        put("longitude", longitude)
        put("accuracyMeters", accuracyMeters?.toDouble())
        put("beaconId", beaconId)
        put("rssi", rssi)
        put("mockSignal", mockSignal.name)
        put("workerId", workerId)
        put("siteId", siteId)
        put("syncState", syncState)
    }

    private fun JSONObject.toEvent(): LocalAttendanceEvent = LocalAttendanceEvent(
        id = getString("id"),
        type = optString("type", LocalAttendanceEvent.TYPE_CHECK_IN),
        timestampMillis = optLong("timestampMillis", 0L),
        latitude = if (isNull("latitude")) null else optDouble("latitude"),
        longitude = if (isNull("longitude")) null else optDouble("longitude"),
        accuracyMeters = if (isNull("accuracyMeters")) null else optDouble("accuracyMeters").toFloat(),
        beaconId = if (isNull("beaconId")) null else optString("beaconId"),
        rssi = if (isNull("rssi")) null else optInt("rssi"),
        mockSignal = runCatching {
            MockSignal.valueOf(optString("mockSignal", MockSignal.UNKNOWN.name))
        }.getOrDefault(MockSignal.UNKNOWN),
        workerId = optString("workerId", LocalAttendanceEvent.WORKER_ID),
        siteId = optString("siteId", LocalAttendanceEvent.SITE_ID),
        syncState = optString("syncState", LocalAttendanceEvent.PENDING_SYNC)
    )

    private companion object {
        const val PREFERENCES_NAME = "smartvolt_attendance"
        const val KEY_EVENTS = "events"
    }
}
