package cl.smartvolt.asistencia.data.local

/**
 * Almacenamiento local de marcas de asistencia.
 *
 * Sirve como contrato para que la capa de datos pueda reemplazarse por Room
 * cuando el equipo integre la persistencia definitiva, sin cambiar la UI.
 */
interface AttendanceLocalStore {

    /** Guarda una marca nueva. */
    fun save(event: LocalAttendanceEvent)

    /** Devuelve las marcas aún no sincronizadas. */
    fun pending(): List<LocalAttendanceEvent>

    /** Cantidad de marcas pendientes de sincronizar. */
    fun count(): Int
}
