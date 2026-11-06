package cl.smartvolt.asistencia.modelo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Maneja el guardado y las consultas de las marcas en Room
@Dao
interface AsistenciaDAO {

    // Guarda una marca nueva y evita duplicados
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(asistencia: Asistencia): Long

    // Devuelve todas las marcas, la mas nueva primero
    @Query("SELECT * FROM asistencias ORDER BY timestampMillis DESC")
    fun observarTodas(): Flow<List<Asistencia>>

    // Devuelve las marcas de un trabajador
    @Query("SELECT * FROM asistencias WHERE workerId = :workerId ORDER BY timestampMillis DESC")
    fun observarPorTrabajador(workerId: String): Flow<List<Asistencia>>

    // Devuelve las marcas de una faena
    @Query("SELECT * FROM asistencias WHERE siteId = :siteId ORDER BY timestampMillis DESC")
    fun observarPorFaena(siteId: String): Flow<List<Asistencia>>

    // Lista las marcas de un trabajador de la mas vieja a la mas nueva
    @Query("SELECT * FROM asistencias WHERE workerId = :workerId ORDER BY timestampMillis ASC")
    suspend fun listarPorTrabajador(workerId: String): List<Asistencia>

    // Devuelve las marcas que todavia no se sincronizan
    @Query("SELECT * FROM asistencias WHERE syncState = 'PENDING_SYNC' ORDER BY timestampMillis ASC")
    fun observarPendientes(): Flow<List<Asistencia>>
}
