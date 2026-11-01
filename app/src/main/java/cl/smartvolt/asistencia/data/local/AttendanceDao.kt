package cl.smartvolt.asistencia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO de marcas de asistencia. Define las operaciones de base de datos para
 * Room/SQLite, de modo que la UI no acceda directamente a la base de datos.
 */
@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: AttendanceEventEntity): Long

    @Query("SELECT * FROM attendance_events ORDER BY timestampMillis DESC")
    suspend fun getAll(): List<AttendanceEventEntity>

    @Query("SELECT * FROM attendance_events WHERE syncState = :state ORDER BY timestampMillis ASC")
    suspend fun getBySyncState(state: String): List<AttendanceEventEntity>

    @Query("SELECT COUNT(*) FROM attendance_events WHERE syncState = :state")
    suspend fun countBySyncState(state: String): Int

    @Query("SELECT * FROM attendance_events WHERE workerId = :workerId ORDER BY timestampMillis DESC")
    suspend fun getByWorker(workerId: String): List<AttendanceEventEntity>

    @Query("SELECT * FROM attendance_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AttendanceEventEntity?

    @Query("UPDATE attendance_events SET syncState = :newState WHERE id = :id")
    suspend fun updateSyncState(id: String, newState: String)

    @Query("DELETE FROM attendance_events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM attendance_events")
    suspend fun totalCount(): Int
}
