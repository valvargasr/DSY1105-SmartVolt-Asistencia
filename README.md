# SmartVolt Asistencia

Aplicación Android para registrar y consultar la asistencia del personal en faena. Es un MVP
académico de DSY1105 (Evaluación Parcial 2) y usa GPS y Bluetooth Low Energy como apoyo para
comprobar la presencia física del trabajador. No es un sistema productivo y no contiene datos
reales de trabajadores.

| Dato | Valor |
|---|---|
| Asignatura | DSY1105 - Desarrollo de Aplicaciones Móviles |
| Evaluación | Evaluación Parcial 2 (EP2) |
| Organización de referencia | Servicio de Ingeniería Smart-Volt SpA |
| Paquete | `cl.smartvolt.asistencia` |
| Plataforma | Android, Kotlin |

## Equipo

| Integrante | Área | Responsabilidad |
|---|---|---|
| Valentina Vargas | A | Interfaz, navegación y pantallas |
| Julián Silva | B | Lógica de negocio, ViewModels, validaciones y almacenamiento |
| Joaquín Rojas | C | Permisos, GPS, Bluetooth BLE y recursos nativos |

## Stack

Kotlin con Jetpack Compose y Material 3, arquitectura MVVM, ViewModel con StateFlow,
Navigation Compose, Room sobre SQLite, corrutinas y pruebas unitarias con JUnit. El control
de versiones está en GitHub y la planificación de tareas en Trello.

## Qué hace la aplicación

La sesión se inicia con cuentas de demostración y el rol decide qué se ve. Hay tres perfiles.

El obrero registra entrada y salida, ve el estado de su jornada y su historial, y solicita
horas extra indicando fecha, horario y motivo. Las solicitudes quedan pendientes hasta que un
administrador las revise.

El capataz tiene un panel con la información del equipo: quiénes están presentes, quiénes
ya salieron y cuántas marcas esperan sincronización. No puede aprobar horas extra.

El administrador además autoriza o rechaza las solicitudes. El rechazo exige un motivo.
Ese motivo se guarda aparte del que escribió el trabajador, para que la tarjeta no confunda
una cosa con la otra.

Solo el administrador puede cambiar el lugar de la faena: nombre, dirección, coordenadas del
centro y radio de marcación. El resto de las cuentas ve ese nombre en el inicio, el historial
y el panel.

La pantalla de marcación pide ubicación y una baliza BLE cercana antes de habilitar el botón.
Además compara la lectura del GPS con el centro de la faena y no registra si el trabajador
está fuera del radio configurado. Si falta algo, explica qué pasa y no guarda nada. Cada marca
nace como pendiente de sincronización y la interfaz lo dice así, porque todavía no hay
servidor que la valide.

## Almacenamiento y datos

La asistencia, las solicitudes y la faena viven en Room, base `smartvolt_attendance.db`, y
sobreviven al cierre de la aplicación. La interfaz `AttendanceLocalStore` separa el resto del
código de la implementación concreta.

Las cuentas son ficticias. La autenticación de demostración no representa un sistema real y
no se deben cargar datos personales, credenciales ni llaves privadas.

## Cómo ejecutar

1. Clonar el repositorio y abrirlo en Android Studio.
2. Esperar la sincronización de Gradle y aceptar el SDK que pida.
3. Elegir la configuración `app` y un emulador o teléfono conectado.
4. Ejecutar con el botón Run.

Desde PowerShell:

```powershell
.\gradlew.bat assembleDebug        # compilar
.\gradlew.bat testDebugUnitTest    # 87 pruebas unitarias
.\gradlew.bat lintDebug            # análisis estático
```

Para el APK firmado se usa Build > Generate Signed App Bundle or APK en Android Studio, con
el keystore fuera del repositorio.

## Limitaciones

GPS y BLE se probaron en emulador con datos simulados. La lectura de una baliza real exige un
teléfono físico y una baliza autorizada, así que la matriz de pruebas de campo sigue
pendiente. La ubicación simulada se advierte, pero no bloquea el registro, y no hay cola de
revisión.

No existe sincronización con backend: todo queda en el teléfono. Retrofit, Hilt y un servicio
REST pertenecen a una etapa posterior. Tampoco se calculan remuneraciones ni liquidaciones.

El detalle de las pruebas de campo, los contratos de ubicación y BLE, y las decisiones sobre
umbrales están en [docs/pruebas-area-c.md](docs/pruebas-area-c.md). La configuración de la
faena y el cálculo del radio se explican en
[docs/faena-configuracion.md](docs/faena-configuracion.md).
