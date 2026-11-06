package cl.smartvolt.asistencia.modelo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "faenas")
data class Faena(
    @PrimaryKey
    val siteId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val hourlyRateClp: Int = 7000,
    val overtimeSurchargePercent: Int = 50,
    val perimeterVersion: Int = 1,
    val active: Boolean = true
)

fun faenaPorDefecto(): Faena = Faena(
    siteId = Asistencia.SITE_ID,
    name = "Faena Andes",
    address = "Camino a Farellones km 12, Lo Barnechea",
    latitude = -33.3500,
    longitude = -70.5200,
    radiusMeters = 300,
    hourlyRateClp = 7000,
    overtimeSurchargePercent = 50
)
