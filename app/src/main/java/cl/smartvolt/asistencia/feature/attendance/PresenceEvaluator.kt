package cl.smartvolt.asistencia.feature.attendance

import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.domain.model.Worksite
import cl.smartvolt.asistencia.domain.validation.GeoDistance
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanResult
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanStatus
import cl.smartvolt.asistencia.native.location.LocationResult
import cl.smartvolt.asistencia.native.location.LocationStatus
import cl.smartvolt.asistencia.native.location.MockSignal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Reglas de presentación del flujo de marcación.
 *
 * Es código Kotlin puro (sin Android) para poder probarlo con tests unitarios
 * comunes. Traduce los resultados nativos a mensajes claros y decide cuándo se
 * habilita el registro.
 *
 * Se exige ubicación lista, baliza encontrada y, cuando la faena está
 * configurada, estar dentro de su radio.
 */
object PresenceEvaluator {

    fun locationMessage(result: LocationResult?, worksite: Worksite? = null): String {
        if (result == null) return "Aún no se ha verificado la ubicación."

        val base = when (result.status) {
            LocationStatus.READY -> {
                val accuracy = result.accuracyMeters
                if (accuracy != null) {
                    "Ubicación lista. Precisión estimada: ${accuracy.toInt()} m."
                } else {
                    "Ubicación lista. El teléfono no informó precisión."
                }
            }

            LocationStatus.PERMISSION_DENIED ->
                "Falta el permiso de ubicación. Actívalo para registrar tu entrada."

            LocationStatus.LOCATION_DISABLED ->
                "La ubicación del teléfono está apagada. Actívala en los ajustes del sistema."

            LocationStatus.TIMEOUT ->
                "No se obtuvo una lectura de GPS a tiempo. Intenta de nuevo."

            LocationStatus.INSUFFICIENT_ACCURACY ->
                "La precisión de la ubicación no es suficiente para validar la presencia."

            LocationStatus.UNAVAILABLE ->
                "La ubicación no está disponible en este dispositivo."

            LocationStatus.ERROR ->
                "Ocurrió un error al leer la ubicación. Intenta de nuevo."
        }

        if (result.status != LocationStatus.READY || worksite == null) return base

        val distance = distanceToWorksite(result, worksite) ?: return base
        val rounded = distance.roundToInt()
        return if (distance <= worksite.radiusMeters) {
            "$base Estás dentro de ${worksite.name} (${rounded} m del centro, radio ${worksite.radiusMeters} m)."
        } else {
            "$base Estás fuera de ${worksite.name} (${rounded} m del centro, radio ${worksite.radiusMeters} m)."
        }
    }

    fun beaconMessage(result: BeaconScanResult?): String {
        if (result == null) return "Aún no se ha escaneado la baliza."

        return when (result.status) {
            BeaconScanStatus.FOUND -> {
                val rssi = result.rssi
                if (rssi != null) {
                    "Baliza detectada. Intensidad de señal: $rssi dBm."
                } else {
                    "Baliza detectada."
                }
            }

            BeaconScanStatus.NOT_FOUND ->
                "No se detectó la baliza autorizada. Acércate a la baliza e intenta de nuevo."

            BeaconScanStatus.PERMISSION_DENIED ->
                "Falta el permiso de Bluetooth para escanear la baliza."

            BeaconScanStatus.BLUETOOTH_OFF ->
                "El Bluetooth está apagado. Actívalo para detectar la baliza."

            BeaconScanStatus.NOT_SUPPORTED ->
                "Este teléfono no soporta Bluetooth de baja energía."

            BeaconScanStatus.TIMEOUT ->
                "El escaneo de la baliza no terminó a tiempo. Intenta de nuevo."

            BeaconScanStatus.ERROR ->
                "Ocurrió un error al escanear la baliza. Intenta de nuevo."
        }
    }

    /** Solo se permite registrar cuando las señales están confirmadas y hay faena. */
    fun canRegister(
        location: LocationResult?,
        beacon: BeaconScanResult?,
        worksite: Worksite? = null
    ): Boolean {
        if (location?.status != LocationStatus.READY) return false
        if (beacon?.status != BeaconScanStatus.FOUND) return false
        if (worksite == null) return true

        val distance = distanceToWorksite(location, worksite) ?: return false
        return distance <= worksite.radiusMeters
    }

    /** Distancia al centro de la faena, o null si falta alguna coordenada. */
    fun distanceToWorksite(location: LocationResult?, worksite: Worksite?): Double? {
        val latitude = location?.latitude ?: return null
        val longitude = location?.longitude ?: return null
        val site = worksite ?: return null
        return GeoDistance.metersBetween(
            latitude,
            longitude,
            site.latitude,
            site.longitude
        )
    }

    /**
     * Motivo por el que no se puede marcar cuando la ubicación está fuera del
     * radio. Devuelve null si está dentro o si todavía no hay datos.
     */
    fun outsideMessage(location: LocationResult?, worksite: Worksite?): String? {
        if (location?.status != LocationStatus.READY) return null
        val site = worksite ?: return null
        val distance = distanceToWorksite(location, site) ?: return null
        if (distance <= site.radiusMeters) return null
        return "No puedes registrar la marca: estás a ${distance.roundToInt()} m del centro de " +
            "${site.name}. El radio permitido es de ${site.radiusMeters} m."
    }

    /**
     * Advertencia cuando el sistema informa ubicación simulada.
     *
     * No acusa fraude: deja la decisión en manos de la revisión humana.
     */
    fun mockWarning(location: LocationResult?): String? =
        if (location?.mockSignal == MockSignal.DETECTED) {
            "El sistema informó una ubicación simulada. La marca quedará en revisión."
        } else {
            null
        }

    fun resultMessage(event: LocalAttendanceEvent): String {
        val hora = SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(Date(event.timestampMillis))
        return "Entrada registrada a las $hora. Guardada en este dispositivo y pendiente de sincronización."
    }

    fun pendingSyncMessage(count: Int): String =
        if (count == 0) {
            "Sin registros pendientes de sincronización."
        } else {
            "Registros pendientes de sincronización: $count."
        }
}
