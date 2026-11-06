package cl.smartvolt.asistencia.util

import cl.smartvolt.asistencia.modelo.Asistencia
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// Ayudas para mostrar fechas y horas en formato chileno
object Fechas {

    private val localeCl = Locale.forLanguageTag("es-CL")

    // Muestra solo la hora, por ejemplo 08:30
    fun hora(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

    // Muestra fecha y hora juntas
    fun fechaHora(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy, HH:mm", localeCl).format(Date(millis))

    // Muestra el dia y el mes en corto
    fun fechaCorta(millis: Long): String =
        SimpleDateFormat("dd MMM", localeCl).format(Date(millis))

    // Muestra la fecha de hoy con el dia de la semana
    fun fechaLarga(): String =
        SimpleDateFormat("EEEE d 'de' MMMM", localeCl)
            .format(Date())
            .replaceFirstChar { it.uppercase() }

    // Muestra el nombre del mes y el año
    fun nombreMes(mes: YearMonth): String {
        val formato = DateTimeFormatter.ofPattern("MMMM yyyy", localeCl)
        return mes.atDay(1).format(formato)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeCl) else it.toString() }
    }

    // Revisa si una marca es de hoy
    fun esHoy(millis: Long, zona: ZoneId = ZoneId.systemDefault()): Boolean {
        val fecha = Instant.ofEpochMilli(millis).atZone(zona).toLocalDate()
        return fecha == LocalDate.now(zona)
    }

    // Revisa si una marca cae en un dia puntual
    fun esDelDia(
        millis: Long,
        fecha: LocalDate,
        zona: ZoneId = ZoneId.systemDefault()
    ): Boolean = Instant.ofEpochMilli(millis).atZone(zona).toLocalDate() == fecha

    // Resume la entrada y la salida del dia
    fun estadoJornada(marcas: List<Asistencia>, fecha: LocalDate = LocalDate.now()): Jornada {
        // con las marcas del día sacamos si hay entrada, salida y si quedó abierta
        val delDia = marcas
            .filter { esDelDia(it.timestampMillis, fecha) }
            .sortedBy { it.timestampMillis }
        val ultima = delDia.lastOrNull()
        return Jornada(
            entrada = delDia.firstOrNull { it.type == Asistencia.TYPE_CHECK_IN }?.timestampMillis,
            salida = delDia.lastOrNull { it.type == Asistencia.TYPE_CHECK_OUT }?.timestampMillis,
            entradaAbierta = ultima?.type == Asistencia.TYPE_CHECK_IN
        )
    }
}

// Resumen de la jornada de un dia
data class Jornada(
    val entrada: Long? = null,
    val salida: Long? = null,
    val entradaAbierta: Boolean = false
) {
    val tieneEntrada: Boolean get() = entrada != null
    val tieneSalida: Boolean get() = salida != null
    val completa: Boolean get() = tieneEntrada && tieneSalida
}
