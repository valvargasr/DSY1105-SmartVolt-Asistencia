package cl.smartvolt.asistencia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.smartvolt.asistencia.data.local.RoomAttendanceStore
import cl.smartvolt.asistencia.domain.model.WorkerRole
import cl.smartvolt.asistencia.feature.attendance.PresenciaScreen
import cl.smartvolt.asistencia.feature.history.HistoryScreen
import cl.smartvolt.asistencia.feature.history.HistoryViewModel
import cl.smartvolt.asistencia.feature.home.HomeScreen
import cl.smartvolt.asistencia.feature.home.HomeViewModel
import cl.smartvolt.asistencia.feature.overtime.OvertimeScreen
import cl.smartvolt.asistencia.feature.overtime.OvertimeViewModel
import cl.smartvolt.asistencia.feature.supervisor.SupervisorScreen
import cl.smartvolt.asistencia.feature.supervisor.SupervisorViewModel
import cl.smartvolt.asistencia.session.DemoAccount
import cl.smartvolt.asistencia.session.DemoDirectory
import cl.smartvolt.asistencia.session.SessionViewModel
import cl.smartvolt.asistencia.ui.components.SectionLabel
import cl.smartvolt.asistencia.ui.components.SmartVoltMark
import cl.smartvolt.asistencia.ui.theme.SmartVoltAsistenciaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartVoltAsistenciaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavegacion(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * Grafo de navegación.
 *
 * El destino inicial depende de la sesión guardada: si la demo ya se abrió
 * antes, se entra directo al inicio del rol correspondiente. El botón atrás
 * siempre vuelve a la pantalla anterior y en el inicio no cierra la app de
 * golpe (evita el reinicio accidental que se observó en las pruebas).
 */
@Composable
fun AppNavegacion(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionViewModel: SessionViewModel = viewModel(factory = SessionViewModel.Factory)
    val session by sessionViewModel.session.collectAsState()
    val store = remember { RoomAttendanceStore(context.applicationContext) }

    NavHost(
        navController = navController,
        startDestination = if (session != null) RUTA_INICIO else RUTA_BIENVENIDA,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(240)) + slideInHorizontally(tween(280)) { it / 12 }
        },
        exitTransition = {
            fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { -it / 24 }
        },
        popEnterTransition = {
            fadeIn(tween(240)) + slideInHorizontally(tween(280)) { -it / 12 }
        },
        popExitTransition = {
            fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 24 }
        }
    ) {
        composable(RUTA_BIENVENIDA) {
            PantallaBienvenida(
                onComenzar = {
                    if (session != null) {
                        navController.navigate(RUTA_INICIO) {
                            popUpTo(RUTA_BIENVENIDA) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(RUTA_LOGIN) { launchSingleTop = true }
                    }
                }
            )
        }

        composable(RUTA_LOGIN) {
            PantallaLogin(
                onSesionIniciada = { cuenta ->
                    sessionViewModel.iniciar(cuenta)
                    navController.navigate(RUTA_INICIO) {
                        // El acceso reemplaza al formulario: volver atrás no debe
                        // mostrar la contraseña escrita otra vez.
                        popUpTo(RUTA_LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(RUTA_INICIO) {
            val cuenta = session
            if (cuenta == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(RUTA_LOGIN) {
                        popUpTo(RUTA_INICIO) { inclusive = true }
                    }
                }
            } else {
                // Atrás en el inicio no cierra la app: primero hay que cerrar sesión.
                BackHandler(enabled = true) { /* permanece en el inicio */ }

                val homeViewModel: HomeViewModel = viewModel(
                    key = "home-${cuenta.workerId}",
                    factory = HomeViewModel.factory(
                        store = store,
                        workerId = cuenta.workerId,
                        siteId = cuenta.siteId
                    )
                )

                HomeScreen(
                    account = cuenta,
                    viewModel = homeViewModel,
                    onRegistrarAsistencia = {
                        navController.navigate(RUTA_PRESENCIA) { launchSingleTop = true }
                    },
                    onVerHistorial = {
                        navController.navigate(RUTA_HISTORIAL) { launchSingleTop = true }
                    },
                    onAbrirPanel = {
                        navController.navigate(RUTA_PANEL) { launchSingleTop = true }
                    },
                    onAbrirHorasExtra = {
                        navController.navigate(RUTA_JORNADA) { launchSingleTop = true }
                    },
                    onCerrarSesion = {
                        sessionViewModel.cerrar()
                        navController.navigate(RUTA_BIENVENIDA) {
                            popUpTo(RUTA_INICIO) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(RUTA_PRESENCIA) {
            val cuenta = session ?: return@composable
            PresenciaScreen(
                workerId = cuenta.workerId,
                siteId = cuenta.siteId,
                siteName = cuenta.siteName,
                roleLabel = cuenta.roleLabel,
                onVolver = { navController.popBackStack() },
                onVerHistorial = {
                    navController.navigate(RUTA_HISTORIAL) { launchSingleTop = true }
                }
            )
        }

        composable(RUTA_HISTORIAL) {
            val cuenta = session ?: return@composable
            val historyViewModel: HistoryViewModel = viewModel(
                key = "history-${cuenta.workerId}",
                factory = HistoryViewModel.factory(
                    store = store,
                    workerId = if (cuenta.role == WorkerRole.WORKER) cuenta.workerId else null,
                    siteId = cuenta.siteId
                )
            )
            HistoryScreen(
                viewModel = historyViewModel,
                workerLabel = if (cuenta.role == WorkerRole.WORKER) {
                    "Trabajador ${cuenta.workerId}"
                } else {
                    "${cuenta.roleLabel} · ${cuenta.siteName}"
                },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(RUTA_PANEL) {
            val cuenta = session ?: return@composable
            val supervisorViewModel: SupervisorViewModel = viewModel(
                key = "panel-${cuenta.siteId}",
                factory = SupervisorViewModel.factory(store, cuenta.siteId)
            )
            SupervisorScreen(
                viewModel = supervisorViewModel,
                siteName = cuenta.siteName,
                onVolver = { navController.popBackStack() }
            )
        }

        composable(RUTA_JORNADA) {
            val cuenta = session ?: return@composable
            val overtimeViewModel: OvertimeViewModel = viewModel(
                factory = OvertimeViewModel.Factory(
                    androidx.compose.ui.platform.LocalContext.current.applicationContext
                        as android.app.Application
                )
            )
            OvertimeScreen(
                viewModel = overtimeViewModel,
                account = cuenta,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}

private const val RUTA_BIENVENIDA = "bienvenida"
private const val RUTA_LOGIN = "login"
private const val RUTA_INICIO = "inicio"
private const val RUTA_PRESENCIA = "presencia"
private const val RUTA_HISTORIAL = "historial"
private const val RUTA_PANEL = "panel"
private const val RUTA_JORNADA = "jornada"

@Composable
fun PantallaBienvenida(
    onComenzar: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A2A52)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0A2A52), Color(0xFF123F78))
                    )
                )
        ) {
            // Manchas de color muy suaves: dan profundidad sin competir con el contenido.
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .offset(x = 170.dp, y = (-170).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .offset(x = (-130).dp, y = 560.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(520)) + slideInVertically(tween(520)) { it / 8 }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SmartVoltMark(size = 78.dp)

                        Spacer(modifier = Modifier.height(22.dp))

                        SectionLabel(
                            text = "Control de asistencia · faena",
                            color = Color.White.copy(alpha = 0.82f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "SmartVolt",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Asistencia en terreno, sin papeleo.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.86f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        FeatureRow(
                            icon = Icons.Outlined.LocationOn,
                            title = "Ubicación GPS",
                            description = "Coordenadas reales del punto de marcaje."
                        )
                        FeatureRow(
                            icon = Icons.Outlined.Bluetooth,
                            title = "Baliza Bluetooth",
                            description = "Compatible con un iPhone emitiendo iBeacon."
                        )
                        FeatureRow(
                            icon = Icons.Outlined.CloudOff,
                            title = "Modo offline",
                            description = "Las marcas se guardan y quedan pendientes."
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = onComenzar,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            Text("Comenzar", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Demo académica · Datos ficticios",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Color.White.copy(alpha = 0.14f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.78f)
            )
        }
    }
}

/**
 * Acceso a la demo.
 *
 * Incluye credenciales reales del directorio demo (validación campo por campo)
 * y botones de rol para entrar directo como obrero, capataz o administrador.
 */
@Composable
fun PantallaLogin(
    onSesionIniciada: (DemoAccount) -> Unit,
    onVolver: () -> Unit = {},
    loginViewModel: LoginViewModel = viewModel()
) {
    val estado by loginViewModel.uiState.collectAsState()
    var mostrarContrasena by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onVolver) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                SectionLabel(text = "Acceso demo")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SmartVoltMark(size = 62.dp)

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Inicia sesión",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Identifícate para registrar tu asistencia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        OutlinedTextField(
                            value = estado.correo,
                            onValueChange = loginViewModel::actualizarCorreo,
                            label = { Text("Correo electrónico") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                if (estado.errorCorreo.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = "Error en el correo",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            isError = estado.errorCorreo.isNotEmpty(),
                            supportingText = {
                                if (estado.errorCorreo.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.ErrorOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(estado.errorCorreo)
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = estado.contrasena,
                            onValueChange = loginViewModel::actualizarContrasena,
                            label = { Text("Contraseña") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { mostrarContrasena = !mostrarContrasena }
                                ) {
                                    Icon(
                                        imageVector = if (mostrarContrasena) {
                                            Icons.Outlined.VisibilityOff
                                        } else {
                                            Icons.Outlined.Visibility
                                        },
                                        contentDescription = if (mostrarContrasena) {
                                            "Ocultar contraseña"
                                        } else {
                                            "Mostrar contraseña"
                                        }
                                    )
                                }
                            },
                            isError = estado.errorContrasena.isNotEmpty(),
                            supportingText = {
                                if (estado.errorContrasena.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.ErrorOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(estado.errorContrasena)
                                    }
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (mostrarContrasena) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                AnimatedVisibility(
                    visible = estado.mensaje.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (estado.mensajeEsError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (estado.mensajeEsError) {
                                    Icons.Outlined.ErrorOutline
                                } else {
                                    Icons.Outlined.CheckCircle
                                },
                                contentDescription = null,
                                tint = if (estado.mensajeEsError) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = estado.mensaje,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (estado.mensajeEsError) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { loginViewModel.ingresar(onSesionIniciada) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciar sesión", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "acceso rápido por rol",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DemoDirectory.accounts.forEach { cuenta ->
                        FilterChip(
                            selected = estado.correo == cuenta.email,
                            onClick = {
                                loginViewModel.ingresarDemo(cuenta, onSesionIniciada)
                            },
                            label = { Text(cuenta.roleLabel) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (cuenta.role) {
                                        WorkerRole.WORKER -> Icons.Outlined.Person
                                        WorkerRole.SUPERVISOR -> Icons.Outlined.Badge
                                        else -> Icons.Outlined.Lock
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor =
                                    MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SectionLabel(text = "Credenciales de la demo")
                        Spacer(modifier = Modifier.height(6.dp))
                        DemoDirectory.accounts.forEach { cuenta ->
                            Text(
                                text = "${cuenta.roleLabel}: ${cuenta.email} · ${cuenta.password}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
