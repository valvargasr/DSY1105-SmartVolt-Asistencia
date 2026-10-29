
package cl.smartvolt.asistencia

import android.util.Patterns
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val errorCorreo: String = "",
    val errorContrasena: String = "",
    val mensaje: String = ""
)

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

    fun validarFormulario() {
        val estado = _uiState.value
        val correoLimpio = estado.correo.trim()

        val errorCorreo = when {
            correoLimpio.isEmpty() ->
                "Ingresa tu correo electrónico."

            !Patterns.EMAIL_ADDRESS.matcher(correoLimpio).matches() ->
                "Ingresa un correo electrónico válido."

            else -> ""
        }

        val errorContrasena = when {
            estado.contrasena.isBlank() ->
                "Ingresa tu contraseña."

            else -> ""
        }

        val mensaje = if (
            errorCorreo.isEmpty() &&
            errorContrasena.isEmpty()
        ) {
            "Formato válido. Falta verificar tus credenciales."
        } else {
            ""
        }

        _uiState.value = estado.copy(
            errorCorreo = errorCorreo,
            errorContrasena = errorContrasena,
            mensaje = mensaje
        )
    }
}
