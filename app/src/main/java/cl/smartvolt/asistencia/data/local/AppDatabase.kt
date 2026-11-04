package cl.smartvolt.asistencia.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos local de la aplicación (Room sobre SQLite).
 *
 * Persiste las marcas de asistencia y las solicitudes de horas extra, y permite
 * operar en modo offline.
 */
@Database(
    entities = [AttendanceEventEntity::class, OvertimeApprovalEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
    abstract fun overtimeApprovalDao(): OvertimeApprovalDao

    companion object {
        /**
         * v3 agrega `decisionNote` para separar la nota del administrador del
         * motivo escrito por el trabajador, sin perder los datos existentes.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE overtime_approvals ADD COLUMN decisionNote TEXT"
                )
            }
        }
    }
}
