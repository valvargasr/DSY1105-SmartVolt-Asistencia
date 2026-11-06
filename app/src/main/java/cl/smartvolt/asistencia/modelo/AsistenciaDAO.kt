package cl.smartvolt.asistencia.modelo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AsistenciaDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(asistencia: Asistencia): Long

    @Query("SELECT * FROM asistencias ORDER BY timestampMillis DESC")
    fun observarTodas(): Flow<List<Asistencia>>

    @Query("SELECT * FROM asistencias WHERE workerId = :workerId ORDER BY timestampMillis DESC")
    fun observarPorTrabajador(workerId: String): Flow<List<Asistencia>>

    @Query("SELECT * FROM asistencias WHERE siteId = :siteId ORDER BY timestampMillis DESC")
    fun observarPorFaena(siteId: String): Flow<List<Asistencia>>

    @Query("SELECT * FROM asistencias WHERE workerId = :workerId ORDER BY timestampMillis ASC")
    suspend fun listarPorTrabajador(workerId: String): List<Asistencia>

    @Query("SELECT * FROM asistencias WHERE syncState = 'PENDING_SYNC' ORDER BY timestampMillis ASC")
    fun observarPendientes(): Flow<List<Asistencia>>
}
