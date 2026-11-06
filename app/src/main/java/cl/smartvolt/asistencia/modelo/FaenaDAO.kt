package cl.smartvolt.asistencia.modelo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FaenaDAO {

    @Query("SELECT * FROM faenas WHERE active = 1 ORDER BY siteId LIMIT 1")
    fun observarActiva(): Flow<Faena?>

    @Query("SELECT * FROM faenas WHERE siteId = :siteId LIMIT 1")
    suspend fun obtener(siteId: String): Faena?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(faena: Faena)
}
