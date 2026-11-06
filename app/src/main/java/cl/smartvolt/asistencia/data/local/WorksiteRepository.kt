package cl.smartvolt.asistencia.data.local

import cl.smartvolt.asistencia.domain.model.Worksite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Acceso a los datos de la faena.
 *
 * Mientras el administrador no guarde nada, se devuelve una faena de demo
 * con radio amplio para que las pruebas en terreno no dependan de un punto
 * exacto. Cuando guarda, esa faena pasa a la base y sobrevive al cierre.
 */
class WorksiteRepository(
    private val dao: WorksiteDao
) {

    fun observar(): Flow<Worksite> =
        dao.observeActive().map { it?.toDomain() ?: defaultWorksite() }

    suspend fun actual(): Worksite =
        dao.getById(DEFAULT_SITE_ID)?.toDomain() ?: defaultWorksite()

    suspend fun guardar(worksite: Worksite) {
        val anterior = dao.getById(worksite.siteId)
        val version = (anterior?.perimeterVersion ?: 0) + 1
        dao.upsert(worksite.copy(perimeterVersion = version).toEntity())
    }

    companion object {

        const val DEFAULT_SITE_ID = "FAENA-DEMO-01"

        fun defaultWorksite(): Worksite = Worksite(
            siteId = DEFAULT_SITE_ID,
            name = "Faena Andes",
            address = "Camino a Farellones km 12, Lo Barnechea",
            latitude = -33.3500,
            longitude = -70.5200,
            radiusMeters = 300,
            hourlyRateClp = 7000,
            overtimeSurchargePercent = 50
        )
    }
}
