package cl.smartvolt.asistencia

import android.util.Patterns
import androidx.lifecycle.ViewModel
import cl.smartvolt.asistencia.session.DemoAccount
import cl.smartvolt.asistencia.session.DemoDirectory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val errorCorreo: String = "",
    val errorContrasena: String = "",
    val mensaje: String = "",
    val mensajeEsError: Boolean = false
)

/**
 * Validación del formulario de acceso.
 *
 * Toda la lógica vive aquí, no en el composable (IE 2.2.1). Las credenciales se
 * comparan contra el directorio demo local; no hay backend de autenticación.
 */
class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun actualizarCorreo(nuevoCorreo: String) {
        _uiState.value = _uiState.value.copy(
            correo = nuevoCorreo,
            errorCorreo = "",
            mensaje = ""
        )
    }

    fun actualizarContrasena(nuevaContrasena: String) {
        _uiState.value = _uiState.value.copy(
            contrasena = nuevaContrasena,
            errorContrasena = "",
            mensaje = ""
        )
    }

    /** Valida campo por campo y devuelve la cuenta cuando las credenciales sirven. */
    fun ingresar(onExito: (DemoAccount) -> Unit) {
        val estado = _uiState.value
        val errorCorreo = when {
            estado.correo.isBlank() -> "Ingresa tu correo electrónico."
            !Patterns.EMAIL_ADDRESS.matcher(estado.correo.trim()).matches() ->
                "El correo no tiene un formato válido."
            else -> ""
        }
        val errorContrasena = when {
            estado.contrasena.isBlank() -> "Ingresa tu contraseña."
            estado.contrasena.length < MIN_PASSWORD -> "La contraseña debe tener al menos $MIN_PASSWORD caracteres."
            else -> ""
        }

        if (errorCorreo.isNotEmpty() || errorContrasena.isNotEmpty()) {
            _uiState.value = estado.copy(
                errorCorreo = errorCorreo,
                errorContrasena = errorContrasena,
                mensaje = "",
                mensajeEsError = false
            )
            return
        }

        val cuenta = DemoDirectory.authenticate(estado.correo.trim(), estado.contrasena)
        if (cuenta == null) {
            _uiState.value = estado.copy(
                errorCorreo = "",
                errorContrasena = "",
                mensaje = "Credenciales no reconocidas. Usa una cuenta de demostración.",
                mensajeEsError = true
            )
            return
        }

        _uiState.value = estado.copy(
            mensaje = "Bienvenido, ${cuenta.displayName}.",
            mensajeEsError = false
        )
        onExito(cuenta)
    }

    /** Acceso rápido para probar cada rol sin escribir credenciales. */
    fun ingresarDemo(cuenta: DemoAccount, onExito: (DemoAccount) -> Unit) {
        _uiState.value = _uiState.value.copy(
            correo = cuenta.email,
            contrasena = cuenta.password,
            errorCorreo = "",
            errorContrasena = "",
            mensaje = "Acceso demo como ${cuenta.roleLabel}.",
            mensajeEsError = false
        )
        onExito(cuenta)
    }

    fun limpiarMensaje() {
        _uiState.value = _uiState.value.copy(mensaje = "")
    }

    private companion object {
        const val MIN_PASSWORD = 6
    }
}
