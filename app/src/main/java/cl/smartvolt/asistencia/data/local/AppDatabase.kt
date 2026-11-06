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
    version = 5,
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

        /**
         * v5 agrega la tarifa por hora y el recargo de las horas extra a la
         * faena. Los valores por defecto cubren las faenas ya guardadas.
         */
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE worksites ADD COLUMN hourlyRateClp INTEGER NOT NULL DEFAULT 7000"
                )
                db.execSQL(
                    "ALTER TABLE worksites ADD COLUMN overtimeSurchargePercent INTEGER NOT NULL DEFAULT 50"
                )
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
    }
}
