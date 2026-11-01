package cl.smartvolt.asistencia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OvertimeApprovalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(approval: OvertimeApprovalEntity)

    @Query("SELECT * FROM overtime_approvals ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<OvertimeApprovalEntity>

    @Query("SELECT * FROM overtime_approvals WHERE workerId = :workerId ORDER BY createdAtMillis DESC")
    suspend fun getByWorker(workerId: String): List<OvertimeApprovalEntity>

    @Query("SELECT * FROM overtime_approvals WHERE status = :status ORDER BY createdAtMillis DESC")
    suspend fun getByStatus(status: String): List<OvertimeApprovalEntity>

    @Query("SELECT * FROM overtime_approvals WHERE approvalId = :id LIMIT 1")
    suspend fun getById(id: String): OvertimeApprovalEntity?

    @Query("UPDATE overtime_approvals SET status = :newStatus, approverId = :approverId, reason = :reason WHERE approvalId = :id")
    suspend fun updateDecision(id: String, newStatus: String, approverId: String, reason: String?)

    @Query("SELECT COUNT(*) FROM overtime_approvals WHERE status = :status")
    suspend fun countByStatus(status: String): Int
}
