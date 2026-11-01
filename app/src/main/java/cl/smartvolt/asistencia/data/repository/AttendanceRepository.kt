package cl.smartvolt.asistencia.data.repository

import cl.smartvolt.asistencia.data.local.AppDatabase
import cl.smartvolt.asistencia.data.local.AttendanceEventEntity
import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.data.local.toDomain
import cl.smartvolt.asistencia.data.local.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio de marcas de asistencia.
 *
 * Unifica el acceso a Room (persistencia local) y, en una fase posterior,
 * al servicio REST. La UI/ViewModel no accede directamente al DAO.
 */
class AttendanceRepository(
    private val database: AppDatabase
) {
    private val dao = database.attendanceDao()

    suspend fun save(event: LocalAttendanceEvent) = withContext(Dispatchers.IO) {
        dao.insert(event.toEntity())
    }

    suspend fun getAll(): List<LocalAttendanceEvent> = withContext(Dispatchers.IO) {
        dao.getAll().map { it.toDomain() }
    }

    suspend fun getPending(): List<LocalAttendanceEvent> = withContext(Dispatchers.IO) {
        dao.getBySyncState(LocalAttendanceEvent.PENDING_SYNC).map { it.toDomain() }
    }

    suspend fun countPending(): Int = withContext(Dispatchers.IO) {
        dao.countBySyncState(LocalAttendanceEvent.PENDING_SYNC)
    }

    suspend fun getById(id: String): LocalAttendanceEvent? = withContext(Dispatchers.IO) {
        dao.getById(id)?.toDomain()
    }

    suspend fun getByWorker(workerId: String): List<LocalAttendanceEvent> = withContext(Dispatchers.IO) {
        dao.getByWorker(workerId).map { it.toDomain() }
    }

    suspend fun updateSyncState(id: String, newState: String) = withContext(Dispatchers.IO) {
        dao.updateSyncState(id, newState)
    }

    suspend fun deleteById(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun totalCount(): Int = withContext(Dispatchers.IO) {
        dao.totalCount()
    }
}
