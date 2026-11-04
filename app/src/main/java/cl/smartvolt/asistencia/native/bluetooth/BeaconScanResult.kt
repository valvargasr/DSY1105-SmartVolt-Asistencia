package cl.smartvolt.asistencia.native.bluetooth

/** Estados posibles de un escaneo de baliza BLE. */
enum class BeaconScanStatus {
    FOUND,
    NOT_FOUND,
    PERMISSION_DENIED,
    BLUETOOTH_OFF,
    NOT_SUPPORTED,
    TIMEOUT,
    ERROR
}

/**
 * Resultado de un escaneo de baliza.
 *
 * [beaconId] identifica el anuncio: el UUID de un iBeacon, el identificador de
 * un Eddystone-UID o el UUID de servicio del formato de laboratorio. No es la
 * dirección física del emisor.
 */
data class BeaconScanResult(
    val status: BeaconScanStatus,
    val beaconId: String? = null,
    val rssi: Int? = null,
    val capturedAtMillis: Long? = null,
    val errorCode: String? = null,
    val format: String? = null,
    val major: Int? = null,
    val minor: Int? = null
) {
    /** Texto corto para la tarjeta de estado de la interfaz. */
    val displayId: String
        get() = when {
            beaconId == null -> "—"
            major != null && minor != null ->
                "$beaconId · $major/$minor"
            else -> beaconId
        }
}
