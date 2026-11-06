package cl.smartvolt.asistencia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorksiteDao {

    @Query("SELECT * FROM worksites WHERE active = 1 ORDER BY siteId LIMIT 1")
    fun observeActive(): Flow<WorksiteEntity?>

    @Query("SELECT * FROM worksites WHERE siteId = :siteId LIMIT 1")
    suspend fun getById(siteId: String): WorksiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(worksite: WorksiteEntity)
}
