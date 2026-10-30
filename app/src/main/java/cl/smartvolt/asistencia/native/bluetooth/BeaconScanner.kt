package cl.smartvolt.asistencia.native.bluetooth

/** Escáner de balizas BLE de la faena. */
interface BeaconScanner {

    /** Indica si el Bluetooth del teléfono está encendido. */
    fun isBluetoothEnabled(): Boolean

    /** Indica si el hardware soporta Bluetooth de baja energía. */
    fun isSupported(): Boolean

    /**
     * Busca la baliza de prueba autorizada.
     *
     * Nunca lanza excepciones: todos los fallos se devuelven como un
     * [BeaconScanResult] con el [BeaconScanResult.status] correspondiente.
     */
    suspend fun scan(): BeaconScanResult
}
