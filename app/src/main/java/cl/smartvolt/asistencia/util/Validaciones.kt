package cl.smartvolt.asistencia.util

import java.time.LocalTime

object Validaciones {

    data class DatosFaena(
        val name: String,
        val address: String,
        val radiusMeters: Int,
        val latitude: Double,
        val longitude: Double
    )

    data class ErroresFaena(
        val name: String = "",
        val address: String = "",
        val radiusMeters: String = "",
        val latitude: String = "",
        val longitude: String = ""
    ) {
        val hayErrores: Boolean
            get() = name.isNotBlank() || address.isNotBlank() ||
                radiusMeters.isNotBlank() || latitude.isNotBlank() ||
                longitude.isNotBlank()
    }

    sealed interface ResultadoFaena {
        data class Valida(val datos: DatosFaena) : ResultadoFaena
        data class Invalida(val errores: ErroresFaena) : ResultadoFaena
    }

    data class DatosHoraExtra(
        val date: java.time.LocalDate,
        val startMillis: Long,
        val endMillis: Long,
        val reason: String
    )

    data class ErroresHoraExtra(
        val desde: String = "",
        val hasta: String = "",
        val motivo: String = "",
        val fecha: String = ""
    ) {
        val hayErrores: Boolean
            get() = desde.isNotBlank() || hasta.isNotBlank() ||
                motivo.isNotBlank() || fecha.isNotBlank()
    }

    sealed interface ResultadoHoraExtra {
        data class Valida(val datos: DatosHoraExtra) : ResultadoHoraExtra
        data class Invalida(val errores: ErroresHoraExtra) : ResultadoHoraExtra
    }

    const val NOMBRE_MINIMO = 3
    const val NOMBRE_MAXIMO = 60
    const val DIRECCION_MINIMO = 5
    const val DIRECCION_MAXIMO = 120
    const val RADIO_MINIMO = 20
    const val RADIO_MAXIMO = 1000
    const val MOTIVO_MINIMO = 5

    private val formatoHora = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")

    fun validarFaena(
        name: String,
        address: String,
        radiusMeters: String,
        latitude: String,
        longitude: String
    ): ResultadoFaena {
        val nombre = name.trim()
        val direccion = address.trim()
        val radio = radiusMeters.trim().toIntOrNull()
        val lat = latitude.trim().replace(',', '.').toDoubleOrNull()
        val lon = longitude.trim().replace(',', '.').toDoubleOrNull()

        val errores = ErroresFaena(
            name = when {
                nombre.isEmpty() -> "Escribe el nombre de la faena."
                nombre.length < NOMBRE_MINIMO ->
                    "El nombre debe tener al menos $NOMBRE_MINIMO caracteres."
                nombre.length > NOMBRE_MAXIMO ->
                    "El nombre no puede superar los $NOMBRE_MAXIMO caracteres."
                else -> ""
            },
            address = when {
                direccion.isEmpty() -> "Escribe una dirección de referencia."
                direccion.length < DIRECCION_MINIMO ->
                    "La dirección debe tener al menos $DIRECCION_MINIMO caracteres."
                direccion.length > DIRECCION_MAXIMO ->
                    "La dirección no puede superar los $DIRECCION_MAXIMO caracteres."
                else -> ""
            },
            radiusMeters = when {
                radio == null -> "El radio debe ser un número entero."
                radio < RADIO_MINIMO || radio > RADIO_MAXIMO ->
                    "El radio debe estar entre $RADIO_MINIMO y $RADIO_MAXIMO metros."
                else -> ""
            },
            latitude = when {
                lat == null -> "La latitud debe ser un número."
                lat < -90.0 || lat > 90.0 -> "La latitud debe estar entre -90 y 90."
                else -> ""
            },
            longitude = when {
                lon == null -> "La longitud debe ser un número."
                lon < -180.0 || lon > 180.0 -> "La longitud debe estar entre -180 y 180."
                else -> ""
            }
        )

        if (errores.hayErrores) return ResultadoFaena.Invalida(errores)

        return ResultadoFaena.Valida(
            DatosFaena(
                name = nombre,
                address = direccion,
                radiusMeters = radio!!,
                latitude = lat!!,
                longitude = lon!!
            )
        )
    }

    fun validarHoraExtra(
        fecha: java.time.LocalDate?,
        desde: String,
        hasta: String,
        motivo: String
    ): ResultadoHoraExtra {
        val inicioTexto = desde.trim()
        val terminoTexto = hasta.trim()
        val razon = motivo.trim()

        val errorDesde = when {
            inicioTexto.isEmpty() -> "Ingresa la hora de inicio."
            !formatoHora.matches(inicioTexto) -> "Usa el formato HH:mm."
            else -> ""
        }
        val errorHasta = when {
            terminoTexto.isEmpty() -> "Ingresa la hora de término."
            !formatoHora.matches(terminoTexto) -> "Usa el formato HH:mm."
            errorDesde.isEmpty() &&
                !LocalTime.parse(terminoTexto).isAfter(LocalTime.parse(inicioTexto)) ->
                "La hora de término debe ser posterior a la de inicio."
            else -> ""
        }
        val errorMotivo = when {
            razon.isEmpty() -> "Explica el motivo de las horas extra."
            razon.length < MOTIVO_MINIMO ->
                "El motivo debe tener al menos $MOTIVO_MINIMO caracteres."
            else -> ""
        }
        val errorFecha = if (fecha == null) "Selecciona una fecha." else ""

        if (errorFecha.isNotEmpty() || errorDesde.isNotEmpty() ||
            errorHasta.isNotEmpty() || errorMotivo.isNotEmpty()
        ) {
            return ResultadoHoraExtra.Invalida(
                ErroresHoraExtra(
                    desde = errorDesde,
                    hasta = errorHasta,
                    motivo = errorMotivo,
                    fecha = errorFecha
                )
            )
        }

        val zona = java.time.ZoneId.systemDefault()
        val inicio = fecha!!.atTime(LocalTime.parse(inicioTexto))
            .atZone(zona).toInstant().toEpochMilli()
        val termino = fecha.atTime(LocalTime.parse(terminoTexto))
            .atZone(zona).toInstant().toEpochMilli()

        return ResultadoHoraExtra.Valida(
            DatosHoraExtra(
                date = fecha,
                startMillis = inicio,
                endMillis = termino,
                reason = razon
            )
        )
    }
}
