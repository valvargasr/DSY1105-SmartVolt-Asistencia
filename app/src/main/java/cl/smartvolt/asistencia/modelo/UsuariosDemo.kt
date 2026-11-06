package cl.smartvolt.asistencia.modelo

data class DemoAccount(
    val email: String,
    val password: String,
    val displayName: String,
    val workerId: String,
    val role: WorkerRole,
    val roleLabel: String,
    val siteId: String,
    val siteName: String,
    val position: String,
    val shiftLabel: String
)

enum class WorkerRole(val label: String) {
    WORKER("Operario"),
    SUPERVISOR("Supervisor"),
    ADMIN("Administrador"),
    HR("Administración/RR.HH."),
    MANAGEMENT("Gerencia")
}

// cuentas fijas para la demo, no hay base de usuarios real
object DemoDirectory {

    const val PASSWORD = "demo1234"

    val accounts: List<DemoAccount> = listOf(
        DemoAccount(
            email = "obrero@smartvolt.cl",
            password = PASSWORD,
            displayName = "Camila Rojas",
            workerId = "OP-001",
            role = WorkerRole.WORKER,
            roleLabel = "Obrero",
            siteId = "FAENA-DEMO-01",
            siteName = "Faena Andes - Frente Norte",
            position = "Operadora de maquinaria",
            shiftLabel = "Turno 08:00 a 17:00"
        ),
        DemoAccount(
            email = "capataz@smartvolt.cl",
            password = PASSWORD,
            displayName = "Rodrigo Salas",
            workerId = "CAP-002",
            role = WorkerRole.SUPERVISOR,
            roleLabel = "Capataz",
            siteId = "FAENA-DEMO-01",
            siteName = "Faena Andes - Frente Norte",
            position = "Jefe de cuadrilla",
            shiftLabel = "Turno 07:30 a 17:30"
        ),
        DemoAccount(
            email = "admin@smartvolt.cl",
            password = PASSWORD,
            displayName = "Valentina Vargas",
            workerId = "ADM-003",
            role = WorkerRole.ADMIN,
            roleLabel = "Administrador",
            siteId = "FAENA-DEMO-01",
            siteName = "Faena Andes - Frente Norte",
            position = "Administración de faena",
            shiftLabel = "Jornada completa"
        )
    )

    fun authenticate(email: String, password: String): DemoAccount? {
        val normalizado = email.trim().lowercase()
        return accounts.firstOrNull {
            it.email.equals(normalizado, ignoreCase = true) && it.password == password
        }
    }

    fun byWorkerId(workerId: String): DemoAccount? =
        accounts.firstOrNull { it.workerId == workerId }

    fun byRole(role: WorkerRole): DemoAccount =
        accounts.first { it.role == role }
}
