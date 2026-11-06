package cl.smartvolt.asistencia.modelo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Maneja las solicitudes de horas extra en Room
@Dao
interface SolicitudHoraExtraDAO {

    // Guarda o actualiza una solicitud
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(solicitud: SolicitudHoraExtra)

    // Devuelve todas las solicitudes, la mas nueva primero
    @Query("SELECT * FROM solicitudes_horas_extra ORDER BY createdAtMillis DESC")
    fun observarTodas(): Flow<List<SolicitudHoraExtra>>

    // Devuelve las solicitudes de un trabajador
    @Query("SELECT * FROM solicitudes_horas_extra WHERE workerId = :workerId ORDER BY createdAtMillis DESC")
    fun observarPorTrabajador(workerId: String): Flow<List<SolicitudHoraExtra>>

    @Query("SELECT * FROM solicitudes_horas_extra WHERE siteId = :siteId ORDER BY createdAtMillis DESC")
    // Devuelve las solicitudes de una faena
    fun observarPorFaena(siteId: String): Flow<List<SolicitudHoraExtra>>

    @Query("SELECT * FROM solicitudes_horas_extra WHERE siteId = :siteId ORDER BY createdAtMillis DESC")
    // Lista las solicitudes de una faena para el reporte
    suspend fun listarPorFaena(siteId: String): List<SolicitudHoraExtra>

    @Query(
        "UPDATE solicitudes_horas_extra SET status = :estado, approverId = :approverId, " +
            "decisionNote = :nota WHERE approvalId = :approvalId"
    )
    // Cambia el estado de una solicitud y guarda quien decidio
    suspend fun decidir(
        approvalId: String,
        estado: String,
        approverId: String,
        nota: String?
    )
}
