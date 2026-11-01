package cl.smartvolt.asistencia.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base de datos local de la aplicación (Room sobre SQLite).
 *
 * Persiste las marcas de asistencia y permite operar en modo offline.
 */
@Database(
    entities = [AttendanceEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
}
