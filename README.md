
# SmartVolt Asistencia

Aplicación móvil Android desarrollada como Producto Mínimo Viable (MVP) académico para el registro y consulta de asistencia en faenas. El proyecto utiliza ubicación GPS y proximidad Bluetooth Low Energy (BLE) como mecanismos de apoyo para verificar la presencia física de los trabajadores.

## 1. Información general

- **Asignatura:** DSY1105 - Desarrollo de Aplicaciones Móviles.
- **Evaluación:** Evaluación Parcial 2 (EP2).
- **Organización de referencia:** Servicio de Ingeniería Smart-Volt SpA.
- **Tipo de proyecto:** MVP académico.
- **Plataforma:** Android.
- **Lenguaje:** Kotlin.
- **Paquete:** `cl.smartvolt.asistencia`.

Este proyecto es una propuesta académica. No corresponde a un sistema productivo ni utiliza información real de trabajadores.

## 2. Equipo y responsabilidades

| Integrante | Área | Responsabilidad |
|---|---|---|
| Valentina Vargas | Área A | Interfaz de usuario, experiencia visual, navegación y pantallas. |
| Julián Silva | Área B | Lógica de negocio, ViewModels, validaciones y almacenamiento local. |
| Joaquín Rojas | Área C | Recursos nativos, permisos, ubicación GPS y Bluetooth BLE. |

## 3. Tecnologías utilizadas

- Kotlin.
- Android Studio.
- Jetpack Compose.
- Material Design 3.
- Arquitectura MVVM.
- ViewModel y StateFlow.
- Navigation Compose.
- Room / SQLite.
- Kotlin Coroutines.
- Pruebas unitarias.
- Git y GitHub.

## 4. Funcionalidades implementadas

### Acceso y perfiles
- Pantalla de bienvenida.
- Acceso mediante cuentas de demostración.
- Perfiles diferenciados por rol.
- Persistencia local de sesión de demostración.

### Operario
- Pantalla principal de jornada.
- Flujo de registro de entrada y salida.
- Visualización del estado de asistencia.
- Consulta del historial.
- Solicitud de horas extras.

### Supervisor
- Panel de supervisión.
- Consulta de información y estados de asistencia del equipo.

### Administrador
- Gestión de solicitudes de horas extras.
- Estados de aprobación y rechazo.
- Registro de observaciones o motivos de decisión.

### Validaciones y experiencia de usuario
- Validaciones de formularios desacopladas de la interfaz.
- Retroalimentación visual.
- Gestión de estados de carga, éxito y error.
- Animaciones mediante componentes de Jetpack Compose.
- Diseño visual basado en Material Design 3.

### Almacenamiento local
- Persistencia de datos mediante Room / SQLite.
- Recuperación de registros almacenados localmente.
- Pruebas unitarias para validaciones.

### Recursos nativos
- Integración de permisos de ubicación.
- Integración de Bluetooth Low Energy (BLE).
- Compatibilidad con lectura de balizas y señal RSSI.
- Funcionalidades de ubicación y visualización asociadas a Mapbox.

## 5. Alcance y limitaciones

La versión actual corresponde a un MVP local.

- Los perfiles de demostración utilizan datos ficticios.
- La persistencia local se realiza mediante Room.
- Las marcas pendientes de sincronización no deben considerarse validadas por un servidor.
- La comprobación GPS y BLE real requiere pruebas en un dispositivo Android físico.
- La detección BLE requiere una baliza física compatible y autorizada.
- La autenticación de demostración no representa un sistema de autenticación productivo.
- La integración con API REST, Retrofit, Hilt y Spring Boot queda para una etapa posterior del proyecto.
- No se realizan cálculos de remuneraciones ni liquidaciones.

## 6. Requisitos para ejecutar el proyecto

- Android Studio.
- Android SDK configurado según la versión del proyecto.
- Gradle Wrapper incluido en el repositorio.
- Emulador Android o dispositivo físico compatible.

## 7. Cómo ejecutar la aplicación

1. Clonar el repositorio.
2. Abrir el proyecto en Android Studio.
3. Esperar la sincronización de Gradle.
4. Seleccionar la configuración `app`.
5. Seleccionar un emulador o dispositivo Android.
6. Presionar **Run** para compilar e iniciar la aplicación.

### Compilación desde terminal

En Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

### Ejecutar pruebas unitarias

```powershell
.\gradlew.bat testDebugUnitTest
```

### Ejecutar análisis Lint

```powershell
.\gradlew.bat lintDebug
```

### Generar versión Release

```powershell
.\gradlew.bat assembleRelease
```

La generación del APK firmado se realiza mediante la opción **Build > Generate Signed App Bundle or APK** de Android Studio.

## 8. Documentación y colaboración

El proyecto utiliza GitHub para el control de versiones y Trello para la planificación y distribución de tareas.

Las funcionalidades, pruebas y limitaciones deben documentarse de acuerdo con el estado real de implementación.

## 9. Protección de datos

La aplicación se desarrolla exclusivamente con datos ficticios o sintéticos para fines académicos.

No se deben incorporar datos personales reales, credenciales, tokens, llaves privadas ni información operacional de la organización.

---

**SmartVolt Asistencia**  
DSY1105 - Desarrollo de Aplicaciones Móviles  
Duoc UC
