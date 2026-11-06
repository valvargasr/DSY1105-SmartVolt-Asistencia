package cl.smartvolt.asistencia.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import cl.smartvolt.asistencia.domain.model.Worksite

/**
 * Entidad Room con los datos de la faena activa.
 *
 * La demo usa una sola faena, así que el identificador no cambia y el
 * administrador solo actualiza sus datos.
 */
@Entity(tableName = "worksites")
data class WorksiteEntity(
    @PrimaryKey
    val siteId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val hourlyRateClp: Int,
    val overtimeSurchargePercent: Int,
    val perimeterVersion: Int,
    val active: Boolean
)

/** Conversión de modelo de dominio a entidad Room. */
fun Worksite.toEntity(): WorksiteEntity = WorksiteEntity(
    siteId = siteId,
    name = name,
    address = address,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    hourlyRateClp = hourlyRateClp,
    overtimeSurchargePercent = overtimeSurchargePercent,
    perimeterVersion = perimeterVersion,
    active = active
)

/** Conversión de entidad Room a modelo de dominio. */
fun WorksiteEntity.toDomain(): Worksite = Worksite(
    siteId = siteId,
    name = name,
    address = address,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    hourlyRateClp = hourlyRateClp,
    overtimeSurchargePercent = overtimeSurchargePercent,
    perimeterVersion = perimeterVersion,
    active = active
)
