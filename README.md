# SmartVolt Asistencia

Aplicación móvil Android para el registro de asistencia en faenas, con verificación de
ubicación GPS y proximidad mediante una baliza Bluetooth Low Energy (BLE).

## 1. Información general

- **Asignatura:** DSY1105 — Desarrollo de Aplicaciones Móviles
- **Evaluación:** Evaluación Parcial 2 (EP2)
- **Organización de referencia:** Servicio de Ingeniería Smart-Volt SpA (caso académico)
- **Tipo de proyecto:** MVP académico. No es un sistema productivo.
- **Paquete de la aplicación:** `cl.smartvolt.asistencia`

## 2. Equipo y responsabilidades

| Integrante | Área | Responsabilidades |
|---|---|---|
| Valentina Vargas | Área A | Diseño de interfaz, experiencia de usuario y pantallas. |
| Julián Silva | Área B | Lógica de negocio, validaciones, almacenamiento local y gestión de datos. |
| Joaquín Rojas | Área C | Recursos nativos de Android (GPS, BLE, permisos, notificaciones) e integración. |

Tablero de tareas: Trello **DSY1105 - SmartVolt Asistencia**.

## 3. Estado de implementación

El equipo informó que la versión anterior de `main` compilaba, superaba 60 pruebas
unitarias y no tenía errores de `lintDebug`. Esta versión incorpora el formulario de
horas extra y 4 pruebas nuevas: se deben volver a ejecutar las comprobaciones antes
de considerar validada esta actualización. **El GPS y el BLE todavía requieren prueba
 en un teléfono físico.**

### Implementado

- Pantalla de bienvenida y pantalla de acceso con validación por campo
  (correo con formato válido y contraseña no vacía), errores junto al campo,
  iconos de error y mensajes visuales por campo.
- **Tres roles de demostración** (obrero, capataz y administrador) con acceso rápido
  desde el login y credenciales ficticias impresas en pantalla. La sesión se guarda en
  `SharedPreferences` y el inicio se adapta al rol.
- **Inicio por rol:** el obrero ve su jornada del día; capataz y administrador ven además
  métricas agregadas de la faena (presentes, salidas, marcas) y accesos al panel y a
  horas extra.
- **Panel de faena** (`SupervisorScreen`) con detalle animado por trabajador y presentes
  calculados desde las marcas del día. Los atrasos y ausencias se muestran como pendientes
  de configuración hasta contar con turnos, tolerancia y nómina aprobados.
- **Horas extra por rol:** el obrero crea una solicitud con fecha, horario y motivo,
  y consulta si está pendiente, aprobada o rechazada. El administrador aprueba o
  rechaza con motivo obligatorio. Las solicitudes se guardan en Room.
- Identidad visual propia (paleta ámbar/teal, tipografía y formas Material 3),
  tarjetas de estado, indicadores de progreso y soporte de modo claro/oscuro.
- Navegación entre pantallas con Navigation Compose y transiciones animadas; el botón
  Atrás del sistema devuelve a la pantalla anterior en lugar de cerrar la aplicación.
- Animaciones de entrada, mensajes de resultado y cambios de estado en la interfaz.
- Pantalla de marcación (`PresenciaScreen`) con estados de permisos, GPS y baliza.
- Solicitud de permisos en contexto con explicación y manejo de rechazo, incluido el
  rechazo permanente con acceso directo a los ajustes de la aplicación.
- Lectura real de ubicación con `LocationManager`: latitud, longitud, precisión,
  instante de la lectura y señal de ubicación simulada.
- **Mapa de Mapbox** (Static Images API) con la posición del marcaje, caché en memoria,
  estados de carga/error y reintento. Requiere permiso `INTERNET`.
- Escaneo BLE real con `BluetoothLeScanner` **sin filtro de servicio**, con ventana de
  escaneo acotada y reporte de RSSI. Acepta tres formatos: **iBeacon** (el que emite la
  app iOS *Locate Beacons*), **Eddystone-UID** y el UUID de servicio de laboratorio.
- **Aviso flotante** cuando la entrada o la salida del día ya estaba registrada, con
  cierre automático y botón de descarte.
- Detección y mensaje para: permiso denegado, ubicación apagada, Bluetooth apagado,
  hardware sin BLE, baliza no encontrada y errores del sistema.
- Guardado local de la marcación con identificador único y estado `PENDING_SYNC`.
- Contador de marcaciones pendientes de sincronización.
- Notificación local al guardar una marcación, con solicitud del permiso
  `POST_NOTIFICATIONS` en Android 13 y posteriores.
- Reglas de presencia en Kotlin puro (`PresenceEvaluator`), separadas de la interfaz.
- 15 pruebas unitarias de esas reglas.
- **Área B (Julián):** Persistencia local con Room/SQLite (entidad, DAO, base de datos, store).
- **Área B (Julián):** Modelos de dominio (AttendanceEvent, WorkerProfile, Worksite, OvertimeApproval, HolidayCalendar).
- **Área B (Julián):** Validaciones desacopladas (`AttendanceValidator`) con 26 pruebas unitarias.
- **Área B (Julián):** Historial de marcas (`HistoryScreen` + `HistoryViewModel`).
- **Área B (Julián):** Registro de salida (check-out) con validación de entrada previa.
- **Área B (Julián):** Clasificador de puntualidad/ausencia configurable (`AttendanceClassifier`).
- **Área B (Julián):** Gestión de horas extra (`OvertimeViewModel`) con persistencia en Room.
- **Área B (Julián):** Calendario de feriados de Chile (`HolidayCalendar`).
- **Área B (Julián):** Panel de supervisor con resumen operativo (`SupervisorViewModel`).
- **Pruebas unitarias:** 4 pruebas adicionales para validar los campos de solicitud de horas extra; la suite contiene 64 casos en total, pendientes de reejecución en el computador de integración.

### Pendiente

- Pruebas en dispositivo físico (GPS y baliza). Ver `docs/pruebas-area-c.md`.
- Activación con doble factor y vinculación del dispositivo.
- Sincronización con API REST (Retrofit, Hilt y Spring Boot), etapa posterior.
- Geocerca y umbral de RSSI: no se inventan valores sin acuerdo con la empresa.
- APK release firmado: requiere generar el keystore (ver sección 9).

## 4. Requisitos del entorno

- Android Studio Rabbit 1 (2026.2.1).
- **JDK 24 o superior.** Verificado con JDK 24 y con el daemon JDK 25 que el wrapper
  aprovisiona automáticamente mediante foojay. No se requiere JDK 17.
- Android SDK Platform **android-37.0** y Build-Tools **37.0.0**.
- Gradle **9.6.0**, incluido mediante el wrapper del repositorio (`gradlew`).
  No es necesario instalar Gradle por separado.
- Variables de entorno `JAVA_HOME` apuntando al JDK 24 (o superior) y `ANDROID_HOME` al SDK.

Versiones declaradas: AGP 9.4.1, Kotlin 2.2.10, Compose BOM 2026.02.01,
`compileSdk` 37, `targetSdk` 37, `minSdk` 24.

No actualizar estas versiones de forma independiente: se revisan en conjunto con el equipo.

## 5. Cómo compilar

Desde la raíz del proyecto, en Windows (PowerShell):

```powershell
$env:JAVA_HOME = "C:\ruta\al\jdk-24"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

# Compila la aplicación de depuración
.\gradlew.bat assembleDebug

# Ejecuta las pruebas unitarias
.\gradlew.bat testDebugUnitTest

# Compila la variante release (sin firmar si no hay keystore.properties)
.\gradlew.bat assembleRelease
```

El APK de depuración queda en `app/build/outputs/apk/debug/app-debug.apk`.

También se puede abrir la carpeta del proyecto en Android Studio, esperar la
sincronización de Gradle y usar **Run** sobre un dispositivo conectado.

## 6. Cómo ejecutar la demostración

1. Instalar el APK en un teléfono Android con GPS y Bluetooth, y conexión a internet
   (la imagen del mapa de Mapbox se descarga por HTTPS).
2. Abrir la aplicación y presionar **Comenzar**.
3. Elegir un rol en **Acceso rápido por rol** (obrero, capataz o administrador) o escribir
   las credenciales demo que aparecen en la misma pantalla. La contraseña de las tres
   cuentas es `demo1234`. Este acceso es solo para la demostración: no representa una
   autenticación real.
4. En el inicio, revisar la jornada del día y, para capataz o administrador, las métricas
   de la faena.
5. Presionar **Registrar entrada** (o el botón equivalente de la tarjeta de jornada).
6. Presionar **Verificar presencia** y conceder los permisos solicitados.
7. Revisar el estado de la ubicación, el mapa de Mapbox y la baliza.
8. Con ambos recursos disponibles, presionar **Registrar entrada**.
9. Volver a intentar registrar la entrada para ver el **aviso flotante** que indica que la
   marca ya existía; se cierra solo a los 7 segundos o con el botón de descarte.
10. Con la entrada del día registrada, presionar **Registrar salida**.
11. Presionar **Ver historial** para revisar las marcas guardadas en el dispositivo.
12. Como obrero, abrir **Horas extra**, completar fecha, horario y motivo, y enviar la solicitud.
13. Cerrar sesión e ingresar como administrador; abrir **Horas extra** y aprobar o rechazar
    la solicitud. Al rechazar, escribir un motivo.
14. Volver a ingresar como obrero y comprobar el estado de la solicitud.
15. Como capataz, abrir el **panel de faena** y revisar los registros del día.

El botón Atrás del sistema devuelve a la pantalla anterior; en el inicio no cierra la
aplicación.

### Baliza de prueba

La aplicación acepta tres formatos de anuncio BLE, sin filtrar por un UUID de servicio:

1. **iBeacon**, que es el que emite la app iOS **Locate Beacons**. Se configura esa app con
   cualquier UUID (por ejemplo `6f1b2c3d-4e5f-4a6b-8c9d-0e1f2a3b4c5d`), un `major` y un
   `minor`, y se deja emitiendo con la pantalla activa.
2. **Eddystone-UID**, servicio `0xFEAA` con trama `0x00`.
3. **UUID de servicio GATT** de laboratorio
   `6f1b2c3d-4e5f-4a6b-8c9d-0e1f2a3b4c5d`, útil con nRF Connect.

`BeaconConfig.EXPECTED_IBEACON_UUID` está en `null` para aceptar el UUID que se configure
en *Locate Beacons* durante la defensa. En producción debe fijarse el UUID autorizado por
la empresa.

Si no hay ningún emisor, la aplicación informa «No se detectó la baliza autorizada» o
«No se encontró la baliza» y no habilita el registro. Ese comportamiento es el esperado,
no un error. El escaneo **no** valida distancia ni geocerca: solo comprueba que el anuncio
existe cerca, por lo que no debe presentarse como antifraude.

## 7. Arquitectura y organización

MVVM con Jetpack Compose, estado expuesto con `StateFlow` y reglas de negocio separadas
de la interfaz.

```
cl.smartvolt.asistencia
├── MainActivity.kt                 Navegación, bienvenida y acceso
├── LoginViewModel.kt               Estado y validación del acceso
├── session/                        Cuentas demo, sesión en SharedPreferences y SessionViewModel
├── ui/theme/                       Tema Material 3, colores y tipografía
├── ui/components/                  Componentes compartidos (tarjetas, estados, cabecera, mapa)
├── feature/attendance/
│   ├── PresenciaScreen.kt          Pantalla de marcación y aviso de duplicado
│   ├── PresenciaViewModel.kt       Orquestación y estado observable
│   ├── PresenciaUiState.kt         Estado inmutable de la pantalla
│   └── PresenceEvaluator.kt        Reglas puras, sin dependencias de Android
├── feature/home/                   Inicio por rol con métricas de la faena
├── feature/history/                Historial de marcas (screen + view model)
├── feature/overtime/               Solicitudes y autorización de horas extra (UI + VM)
├── feature/supervisor/             Panel de faena por trabajador (UI + VM)
├── data/local/
│   ├── LocalAttendanceEvent.kt     Modelo de la marcación
│   ├── AttendanceLocalStore.kt     Puerto de persistencia
│   ├── RoomAttendanceStore.kt      Implementación Room/SQLite
│   ├── AppDatabase.kt              Base de datos (asistencia y horas extra)
│   ├── AttendanceDao.kt            Consultas de marcaciones
│   ├── AttendanceEventEntity.kt    Entidad Room de marcaciones
│   └── OvertimeApproval*.kt        Entidad y DAO de horas extra
├── data/repository/                Repositorio de asistencia
├── domain/model/                   Modelos de dominio y calendario de feriados
├── domain/validation/              AttendanceValidator, AttendanceClassifier y HolidayCalendar
└── native/
    ├── location/                   LocationProvider y AndroidLocationProvider
    ├── bluetooth/                  BeaconScanner, AndroidBeaconScanner (iBeacon/Eddystone)
    ├── maps/                       MapboxConfig y URL de Static Images
    ├── permissions/                PermissionChecker y PermissionStatus
    └── notifications/              AttendanceNotifier
```

Los puertos (`LocationProvider`, `BeaconScanner`, `AttendanceLocalStore`) declaran que no
lanzan excepciones: devuelven un objeto de resultado con estado y código de error. Esto
permite sustituirlos por implementaciones de prueba.

La interfaz usa una identidad visual propia sobre Material 3: ámbar como color principal,
teal para estados correctos, rust para advertencias y una escala tipográfica con jerarquía
clara. Los componentes compartidos de `ui/components/` concentran las tarjetas de estado,
las píldoras de estado, la cabecera y los estados vacíos para mantener consistencia entre
pantallas. El modo oscuro se resuelve con la misma paleta y no depende de colores dinámicos
del sistema.

## 8. Pruebas

```powershell
.\gradlew.bat testDebugUnitTest
```

La suite incluye **64 pruebas unitarias**: `PresenceEvaluatorTest` (15), `AttendanceValidatorTest`
(26), `AttendanceClassifierTest` (11), `HolidayCalendarTest` (7), `OvertimeFormValidatorTest` (4)
y `ExampleUnitTest` (1).

`PresenceEvaluatorTest` cubre 15 casos: ausencia de lecturas, GPS disponible, baliza
detectada, baliza ausente, permiso denegado, ubicación apagada, Bluetooth apagado, tiempo
de espera agotado, precisión informada, ubicación simulada y mensajes de sincronización.

La matriz de pruebas en dispositivo físico y su bitácora están en `docs/pruebas-area-c.md`.

La compilación de verificación incluye además `assembleDebug` y `lintDebug`:

```powershell
.\gradlew.bat --no-configuration-cache testDebugUnitTest assembleDebug lintDebug
```

El equipo debe volver a ejecutar `testDebugUnitTest`, `assembleDebug` y `lintDebug` después
 de integrar estos cambios. El resultado de esas ejecuciones debe registrarse desde el
 computador de integración; no se debe asumir que una compilación anterior valida cambios nuevos.

## 9. Firma del APK release

El keystore y sus contraseñas **no se suben al repositorio** (están en `.gitignore`).

1. Copiar `keystore.properties.example` como `keystore.properties` en la raíz del proyecto.
2. Generar el keystore:

   ```
   keytool -genkeypair -v -keystore smartvolt-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias smartvolt
   ```

3. Completar las contraseñas en `keystore.properties`.
4. Ejecutar `.\gradlew.bat assembleRelease`.

Sin `keystore.properties` la compilación sigue funcionando y genera un APK sin firmar,
que no se puede instalar.

## 10. Privacidad y tratamiento de datos

- Se usan identificadores ficticios: `OP-001` y `FAENA-DEMO-01`. No hay nombres, RUT,
  correos ni datos personales reales.
- **Red:** el permiso `INTERNET` se usa únicamente para descargar la imagen estática
  del mapa desde la API de Mapbox. Las coordenadas del punto de marcaje viajan a Mapbox
  en la URL de esa imagen; no hay backend propio ni otros envíos.
- El token de Mapbox incluido es un token **público de demostración**, no el token
  privado de descarga de Gradle. Debe rotarse y restringirse antes de publicar la app.
- El respaldo automático está deshabilitado (`allowBackup="false"`); la base de datos Room y
  las preferencias quedan excluidas de las reglas de respaldo y transferencia.
- La ubicación simulada se informa como una señal de revisión, no como una acusación de
  fraude. No se promete una detección infalible.
- No se guardan contraseñas, tokens ni claves en el repositorio.

## 11. Alcance y limitaciones

- El registro exige ubicación disponible y baliza detectada, pero **todavía no compara la
  posición con un perímetro de faena ni exige una precisión o un RSSI mínimo**. Esos
  umbrales dependen de decisiones pendientes con el profesor y la contraparte.
- El mapa de Mapbox es una **imagen estática** para dar contexto visual de la marca. No
  dibuja la geocerca ni valida que el punto esté dentro de la faena.
- El escaneo BLE confirma que el anuncio existe cerca; no mide distancia real.
- No hay validación de presencia en servidor. El estado `PENDING_SYNC` significa guardado
  local pendiente de sincronizar, nunca validado por un backend.
- El UUID de la baliza es provisional y clonable: no constituye una prueba criptográfica
  de identidad.
- La persistencia actual usa Room/SQLite (`smartvolt_attendance.db`).
  La implementación legada `SharedPreferencesAttendanceStore` fue eliminada; las marcas
  actuales se guardan mediante Room/SQLite.
- El acceso de demostración no implementa autenticación ni doble factor.

---

**Asignatura:** DSY1105 — Desarrollo de Aplicaciones Móviles
**Proyecto:** SmartVolt Asistencia
