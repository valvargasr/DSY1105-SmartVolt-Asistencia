package cl.smartvolt.asistencia.modelo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SolicitudHoraExtraDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(solicitud: SolicitudHoraExtra)

    @Query("SELECT * FROM solicitudes_horas_extra ORDER BY createdAtMillis DESC")
    fun observarTodas(): Flow<List<SolicitudHoraExtra>>

    @Query("SELECT * FROM solicitudes_horas_extra WHERE workerId = :workerId ORDER BY createdAtMillis DESC")
    fun observarPorTrabajador(workerId: String): Flow<List<SolicitudHoraExtra>>

    @Query("SELECT * FROM solicitudes_horas_extra WHERE siteId = :siteId ORDER BY createdAtMillis DESC")
    fun observarPorFaena(siteId: String): Flow<List<SolicitudHoraExtra>>

    @Query("SELECT * FROM solicitudes_horas_extra WHERE siteId = :siteId ORDER BY createdAtMillis DESC")
    suspend fun listarPorFaena(siteId: String): List<SolicitudHoraExtra>

    @Query(
        "UPDATE solicitudes_horas_extra SET status = :estado, approverId = :approverId, " +
            "decisionNote = :nota WHERE approvalId = :approvalId"
    )
    suspend fun decidir(
        approvalId: String,
        estado: String,
        approverId: String,
        nota: String?
    )
}
