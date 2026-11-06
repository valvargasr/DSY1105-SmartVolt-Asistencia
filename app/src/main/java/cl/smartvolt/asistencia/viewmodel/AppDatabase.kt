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
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun asistenciaDao(): AsistenciaDAO

    abstract fun faenaDao(): FaenaDAO

    abstract fun solicitudHoraExtraDao(): SolicitudHoraExtraDAO

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartvolt.db"
                ).build().also { instancia = it }
            }
    }
}
