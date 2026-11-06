package cl.smartvolt.asistencia.data.local

import android.content.Context
import androidx.room.Room

/**
 * Punto único de apertura de la base local.
 *
 * Varias pantallas usan el mismo archivo, así que las migraciones se
 * declaran en un solo lugar. Si una pantalla abriera la base con su propia
 * configuración, las demás podrían quedar con un esquema distinto.
 */
object AppDatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "smartvolt_attendance.db"
            )
                .addMigrations(*AppDatabase.MIGRATIONS)
                .build()
                .also { instance = it }
        }
}
