package cl.smartvolt.asistencia.modelo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Maneja la faena activa guardada en Room
@Dao
interface FaenaDAO {

    // Observa la faena activa para mostrarla en pantalla
    @Query("SELECT * FROM faenas WHERE active = 1 ORDER BY siteId LIMIT 1")
    fun observarActiva(): Flow<Faena?>

    // Busca una faena por su identificador
    @Query("SELECT * FROM faenas WHERE siteId = :siteId LIMIT 1")
    suspend fun obtener(siteId: String): Faena?

    // Guarda o reemplaza la faena
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(faena: Faena)
}
