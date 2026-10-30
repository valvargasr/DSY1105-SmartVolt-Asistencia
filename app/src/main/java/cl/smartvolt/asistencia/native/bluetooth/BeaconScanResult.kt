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
 * [beaconId] es el UUID de servicio anunciado por la baliza, no la dirección
 * física del dispositivo emisor. El UUID de prueba es ficticio y se reemplaza
 * cuando la contraparte entregue el identificador autorizado.
 */
data class BeaconScanResult(
    val status: BeaconScanStatus,
    val beaconId: String? = null,
    val rssi: Int? = null,
    val capturedAtMillis: Long? = null,
    val errorCode: String? = null
)
