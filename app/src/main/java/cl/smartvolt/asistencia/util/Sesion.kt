package cl.smartvolt.asistencia.util

import cl.smartvolt.asistencia.modelo.DemoAccount

// Guarda la cuenta activa mientras la app esta abierta
object Sesion {
    var cuenta: DemoAccount? = null
}
