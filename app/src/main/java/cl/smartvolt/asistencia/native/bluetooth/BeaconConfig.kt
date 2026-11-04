package cl.smartvolt.asistencia.native.bluetooth

/**
 * Parámetros del escaneo BLE de la demo.
 *
 * El escáner acepta tres formatos para poder usar un iPhone con la app
 * **Locate Beacons** como emisor:
 *
 * 1. **iBeacon** (el formato que emite Locate Beacons en iOS): datos de
 *    fabricante de Apple (`0x004C`) con tipo `0x02` y largo `0x15`.
 * 2. **Eddystone-UID**: servicio `0xFEAA` con trama `0x00`.
 * 3. **UUID de servicio GATT** de laboratorio, usado antes por la app.
 *
 * [EXPECTED_IBEACON_UUID] queda en `null` para aceptar el UUID que se configure
 * en Locate Beacons durante las pruebas. En producción debe fijarse el UUID
 * autorizado por la contraparte.
 */
object BeaconConfig {

    /** UUID de servicio del formato de laboratorio (compatibilidad). */
    const val TEST_SERVICE_UUID: String = "6f1b2c3d-4e5f-4a6b-8c9d-0e1f2a3b4c5d"

    /** Identificador de fabricante de Apple en los anuncios iBeacon. */
    const val IBEACON_COMPANY_ID: Int = 0x004C

    /** Servicio Eddystone. */
    const val EDDYSTONE_SERVICE_UUID: String =
        "0000feaa-0000-1000-8000-00805f9b34fb"

    /** Ventana de escaneo: suficiente para que el iPhone emita un anuncio. */
    const val SCAN_WINDOW_MILLIS: Long = 12_000L

    /**
     * UUID iBeacon esperado. `null` acepta cualquier UUID configurado en
     * Locate Beacons, útil mientras se prepara la baliza de la faena.
     */
    val EXPECTED_IBEACON_UUID: String? = null

    /** Sin umbral inventado: la contraparte no ha definido un RSSI mínimo. */
    val MIN_RSSI: Int? = null
}
