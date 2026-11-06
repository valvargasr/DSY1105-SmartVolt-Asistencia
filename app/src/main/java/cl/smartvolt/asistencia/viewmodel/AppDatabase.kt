package cl.smartvolt.asistencia.viewmodel

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cl.smartvolt.asistencia.modelo.Asistencia
import cl.smartvolt.asistencia.modelo.AsistenciaDAO
import cl.smartvolt.asistencia.modelo.Faena
import cl.smartvolt.asistencia.modelo.FaenaDAO
import cl.smartvolt.asistencia.modelo.SolicitudHoraExtra
import cl.smartvolt.asistencia.modelo.SolicitudHoraExtraDAO

@Database(
    entities = [Asistencia::class, Faena::class, SolicitudHoraExtra::class],
    version = 2,
    exportSchema = false
)
// Base de datos local con Room
abstract class AppDatabase : RoomDatabase() {

    // Acceso a las marcas de asistencia
    abstract fun asistenciaDao(): AsistenciaDAO

    // Acceso a la faena configurada
    abstract fun faenaDao(): FaenaDAO

    // Acceso a las solicitudes de horas extra
    abstract fun solicitudHoraExtraDao(): SolicitudHoraExtraDAO

    // Mantiene una sola instancia de la base para toda la app
    companion object {
        // una sola instancia para toda la app, si no Room se queja
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartvolt.db"
                ).fallbackToDestructiveMigration().build().also { instancia = it }
            }
    }
}
