package cl.smartvolt.asistencia.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos local de la aplicación (Room sobre SQLite).
 *
 * Persiste las marcas de asistencia, las solicitudes de horas extra y los
 * datos de la faena. Permite operar sin conexión.
 */
@Database(
    entities = [
        AttendanceEventEntity::class,
        OvertimeApprovalEntity::class,
        WorksiteEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun attendanceDao(): AttendanceDao

    abstract fun overtimeApprovalDao(): OvertimeApprovalDao

    abstract fun worksiteDao(): WorksiteDao

    companion object {

        /**
         * v3 agrega decisionNote para separar la nota del administrador del
         * motivo escrito por el trabajador, sin perder los datos existentes.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE overtime_approvals ADD COLUMN decisionNote TEXT")
            }
        }

        /**
         * v4 agrega la tabla de faenas. La faena se crea al guardar por
         * primera vez, así que no se siembra ninguna fila aquí.
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `worksites` (" +
                        "`siteId` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`address` TEXT NOT NULL, " +
                        "`latitude` REAL NOT NULL, " +
                        "`longitude` REAL NOT NULL, " +
                        "`radiusMeters` INTEGER NOT NULL, " +
                        "`perimeterVersion` INTEGER NOT NULL, " +
                        "`active` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`siteId`))"
                )
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_2_3, MIGRATION_3_4)
    }
}
