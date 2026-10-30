package cl.smartvolt.asistencia.feature.attendance

import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanResult
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanStatus
import cl.smartvolt.asistencia.native.location.LocationResult
import cl.smartvolt.asistencia.native.location.LocationStatus
import cl.smartvolt.asistencia.native.location.MockSignal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reglas de presentación del flujo de marcación.
 *
 * Es código Kotlin puro (sin Android) para poder probarlo con tests unitarios
 * comunes. Traduce los resultados nativos a mensajes claros y decide cuándo se
 * habilita el registro.
 *
 * Ningún umbral está escondido aquí: solo se exige ubicación lista y baliza
 * encontrada, que son señales concretas, no tolerancias inventadas.
 */
object PresenceEvaluator {

    fun locationMessage(result: LocationResult?): String {
        if (result == null) return "Aún no se ha verificado la ubicación."

        return when (result.status) {
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

    /** Solo se permite registrar cuando ambas señales están confirmadas. */
    fun canRegister(
        location: LocationResult?,
        beacon: BeaconScanResult?
    ): Boolean =
        location?.status == LocationStatus.READY &&
            beacon?.status == BeaconScanStatus.FOUND

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
