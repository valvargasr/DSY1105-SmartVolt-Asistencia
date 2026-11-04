package cl.smartvolt.asistencia.data.local

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Implementación de [AttendanceLocalStore] sobre Room/SQLite.
 *
 * Reemplaza a [SharedPreferencesAttendanceStore] y garantiza que las marcas
 * sobrevivan al cierre de la aplicación. Las operaciones de Room son suspend,
 * por lo que se envuelven en runBlocking cuando se invocan desde la interfaz
 * síncrona; el ViewModel ya ejecuta estas llamadas en Dispatchers.IO.
 */
class RoomAttendanceStore(context: Context) : AttendanceLocalStore {

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "smartvolt_attendance.db"
    ).addMigrations(AppDatabase.MIGRATION_2_3).fallbackToDestructiveMigration().build()

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
            dao.getBySyncState(LocalAttendanceEvent.PENDING_SYNC).map { it.toDomain() }
        }

    @Synchronized
    override fun count(): Int =
        runBlocking(Dispatchers.IO) {
            dao.countBySyncState(LocalAttendanceEvent.PENDING_SYNC)
        }
}
