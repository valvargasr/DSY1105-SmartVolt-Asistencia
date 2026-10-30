package cl.smartvolt.asistencia.native.bluetooth

/**
 * Configuración del escaneo de baliza.
 *
 * [TEST_SERVICE_UUID] es un identificador PROVISIONAL de laboratorio. No
 * corresponde a ninguna baliza real de Smart-Volt: debe reemplazarse por el
 * UUID autorizado que entregue la contraparte (ver docs/pruebas-area-c.md).
 *
 * [MIN_RSSI] se deja en null a propósito: no se inventa un umbral de señal.
 * Cuando exista un umbral aprobado, basta asignarlo aquí y el escáner pasará a
 * exigir esa intensidad mínima.
 */
object BeaconConfig {

    const val TEST_SERVICE_UUID: String = "6f1b2c3d-4e5f-4a6b-8c9d-0e1f2a3b4c5d"

    /** Ventana máxima de escaneo antes de informar que no se encontró la baliza. */
    const val SCAN_WINDOW_MILLIS: Long = 10_000L

    /** Umbral mínimo de RSSI aceptado. null = aceptar la primera coincidencia. */
    val MIN_RSSI: Int? = null
}
