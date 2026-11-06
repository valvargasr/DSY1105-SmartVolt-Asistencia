package cl.smartvolt.asistencia.domain.validation

/**
 * Datos del formulario de faena tal como llegan desde la pantalla.
 *
 * Los numéricos se reciben como texto para poder explicar el error en el
 * mismo campo que escribió el administrador.
 */
data class WorksiteInput(
    val name: String,
    val address: String,
    val radiusMeters: String,
    val latitude: String,
    val longitude: String
)

data class WorksiteErrors(
    val name: String = "",
    val address: String = "",
    val radiusMeters: String = "",
    val latitude: String = "",
    val longitude: String = ""
) {
    val hayErrores: Boolean
        get() = name.isNotBlank() ||
            address.isNotBlank() ||
            radiusMeters.isNotBlank() ||
            latitude.isNotBlank() ||
            longitude.isNotBlank()
}

data class WorksiteValidated(
    val name: String,
    val address: String,
    val radiusMeters: Int,
    val latitude: Double,
    val longitude: Double
)

sealed interface WorksiteValidation {
    data class Valid(val data: WorksiteValidated) : WorksiteValidation
    data class Invalid(val errors: WorksiteErrors) : WorksiteValidation
}

/**
 * Reglas del formulario de faena.
 *
 * Solo se valida lo que la app necesita para calcular la geocerca: un nombre
 * reconocible, una dirección de referencia, un radio razonable y coordenadas
 * dentro del rango del planeta. No se inventan reglas de negocio adicionales.
 */
object WorksiteValidator {

    const val NOMBRE_MINIMO = 3
    const val NOMBRE_MAXIMO = 60
    const val DIRECCION_MINIMO = 5
    const val DIRECCION_MAXIMO = 120
    const val RADIO_MINIMO = 20
    const val RADIO_MAXIMO = 1000

    fun validate(input: WorksiteInput): WorksiteValidation {
        val name = input.name.trim()
        val address = input.address.trim()
        val radius = input.radiusMeters.trim().toIntOrNull()
        val latitude = input.latitude.trim().replace(',', '.').toDoubleOrNull()
        val longitude = input.longitude.trim().replace(',', '.').toDoubleOrNull()

        val nameError = when {
            name.isEmpty() -> "Escribe el nombre de la faena."
            name.length < NOMBRE_MINIMO -> "El nombre debe tener al menos $NOMBRE_MINIMO caracteres."
            name.length > NOMBRE_MAXIMO -> "El nombre no puede superar los $NOMBRE_MAXIMO caracteres."
            else -> ""
        }

        val addressError = when {
            address.isEmpty() -> "Escribe una dirección de referencia."
            address.length < DIRECCION_MINIMO -> "La dirección debe tener al menos $DIRECCION_MINIMO caracteres."
            address.length > DIRECCION_MAXIMO -> "La dirección no puede superar los $DIRECCION_MAXIMO caracteres."
            else -> ""
        }

        val radiusError = when {
            radius == null -> "El radio debe ser un número entero."
            radius < RADIO_MINIMO || radius > RADIO_MAXIMO ->
                "El radio debe estar entre $RADIO_MINIMO y $RADIO_MAXIMO metros."
            else -> ""
        }

        val latitudeError = when {
            latitude == null -> "La latitud debe ser un número."
            latitude < -90.0 || latitude > 90.0 -> "La latitud debe estar entre -90 y 90."
            else -> ""
        }

        val longitudeError = when {
            longitude == null -> "La longitud debe ser un número."
            longitude < -180.0 || longitude > 180.0 -> "La longitud debe estar entre -180 y 180."
            else -> ""
        }

        val errors = WorksiteErrors(
            name = nameError,
            address = addressError,
            radiusMeters = radiusError,
            latitude = latitudeError,
            longitude = longitudeError
        )

        if (errors.hayErrores) return WorksiteValidation.Invalid(errors)

        return WorksiteValidation.Valid(
            WorksiteValidated(
                name = name,
                address = address,
                radiusMeters = radius!!,
                latitude = latitude!!,
                longitude = longitude!!
            )
        )
    }
}
