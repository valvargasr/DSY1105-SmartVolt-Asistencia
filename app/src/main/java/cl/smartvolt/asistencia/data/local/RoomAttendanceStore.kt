package cl.smartvolt.asistencia.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Implementación de AttendanceLocalStore sobre Room/SQLite.
 *
 * Guarda las marcas de asistencia localmente para permitir
 * el funcionamiento sin conexión.
 */
class RoomAttendanceStore(context: Context) : AttendanceLocalStore {

    private val database: AppDatabase = AppDatabaseProvider.get(context)

    private val dao: AttendanceDao = database.attendanceDao()

    @Synchronized
    override fun save(event: LocalAttendanceEvent) {
        runBlocking(Dispatchers.IO) {
            dao.insert(event.toEntity())
        }
    }

    @Synchronized
    override fun all(): List<LocalAttendanceEvent> =
        runBlocking(Dispatchers.IO) {
            dao.getAll().map { it.toDomain() }
        }

    @Synchronized
    override fun pending(): List<LocalAttendanceEvent> =
        runBlocking(Dispatchers.IO) {
            dao.getBySyncState(LocalAttendanceEvent.PENDING_SYNC)
                .map { it.toDomain() }
        }

    @Synchronized
    override fun count(): Int =
        runBlocking(Dispatchers.IO) {
            dao.countBySyncState(LocalAttendanceEvent.PENDING_SYNC)
        }
}