package cl.smartvolt.asistencia.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import cl.smartvolt.asistencia.modelo.DemoAccount
import cl.smartvolt.asistencia.modelo.DemoDirectory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Datos del formulario de login
data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val errorCorreo: String = "",
    val errorContrasena: String = "",
    val mensaje: String = "",
    val mensajeEsError: Boolean = false,
    val mostrarClave: Boolean = false
)

// Valida el correo y la contrasena contra las cuentas demo
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Guarda el correo y limpia el error anterior
    fun actualizarCorreo(valor: String) {
        _uiState.value = _uiState.value.copy(correo = valor, errorCorreo = "", mensaje = "")
    }

    // Guarda la contrasena y limpia el error anterior
    fun actualizarContrasena(valor: String) {
        _uiState.value = _uiState.value.copy(contrasena = valor, errorContrasena = "", mensaje = "")
    }

    // Muestra u oculta la contrasena
    fun cambiarClaveVisible() {
        _uiState.value = _uiState.value.copy(mostrarClave = !_uiState.value.mostrarClave)
    }

    // Revisa los datos y devuelve la cuenta si existe
    fun ingresar(): DemoAccount? {
        val estado = _uiState.value
        // login contra las cuentas demo, todavía no hay backend
        val errorCorreo = when {
            estado.correo.isBlank() -> "Ingresa tu correo electrónico."
            !Patterns.EMAIL_ADDRESS.matcher(estado.correo.trim()).matches() ->
                "El correo no tiene un formato válido."
            else -> ""
        }
        val errorContrasena = when {
            estado.contrasena.isBlank() -> "Ingresa tu contraseña."
            estado.contrasena.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
            else -> ""
        }

        if (errorCorreo.isNotEmpty() || errorContrasena.isNotEmpty()) {
            _uiState.value = estado.copy(
                errorCorreo = errorCorreo,
                errorContrasena = errorContrasena,
                mensaje = "",
                mensajeEsError = false
            )
            return null
        }

        val cuenta = DemoDirectory.authenticate(estado.correo.trim(), estado.contrasena)
        if (cuenta == null) {
            _uiState.value = estado.copy(
                errorCorreo = "",
                errorContrasena = "",
                mensaje = "Credenciales no reconocidas. Usa una cuenta de demostración.",
                mensajeEsError = true
            )
            return null
        }

        _uiState.value = estado.copy(
            mensaje = "Bienvenido, ${cuenta.displayName}.",
            mensajeEsError = false
        )
        return cuenta
    }

    // Entra directo con una cuenta de demostracion
    fun accesoDemo(cuenta: DemoAccount): DemoAccount {
        // los botones demo rellenan el formulario y entran directo
        _uiState.value = _uiState.value.copy(
            correo = cuenta.email,
            contrasena = cuenta.password,
            errorCorreo = "",
            errorContrasena = "",
            mensaje = "Acceso demo como ${cuenta.roleLabel}.",
            mensajeEsError = false
        )
        return cuenta
    }
}
