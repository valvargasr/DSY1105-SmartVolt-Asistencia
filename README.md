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

Kotlin con Jetpack Compose y Material 3, arquitectura MVVM simple, ViewModel con StateFlow,
Navigation Compose, Room sobre SQLite, corrutinas y pruebas unitarias con JUnit. El control
de versiones está en GitHub y la planificación de tareas en Trello.

## Estructura del proyecto

El código está separado en cuatro capas simples, sin repository ni factories:

| Carpeta | Contenido |
|---|---|
| `modelo/` | Entidades Room, DAOs, cuentas demo y cálculo del reporte mensual |
| `viewmodel/` | Base de datos y ViewModels que leen la sesión y usan los DAO |
| `ui/view/` | Pantallas de Compose y navegación |
| `ui/components/` | Tarjetas, etiquetas y mapa reutilizables |
| `ui/theme/` | Colores, tipografía y tema azul |
| `util/` | Sesión, GPS, baliza, permisos, geocerca, fechas y exportaciones |

La navegación principal tiene tres rutas: `splash`, `login` y `principal`. Dentro de
`principal` hay cuatro pestañas fijas: Inicio, Asistencia, Historial y Gestión. Las pantallas
`panel`, `faena`, `reporte` y `horas_extra` se abren desde las pestañas y no muestran la barra
inferior.

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

Ese mismo rol puede abrir el registro del mes, cambiar de mes con las flechas y guardar el
detalle en un CSV o en un PDF. El total suma la asistencia y las horas extra aprobadas; las
solicitudes pendientes se informan aparte y no suman horas. Las entradas sin salida quedan
marcadas y tampoco se suman.

La tarifa por hora y el recargo de las horas extra se configuran en la misma pantalla de la
faena. Con esos valores el registro estima el monto de cada trabajador y el total del mes. Es
una estimación para la demo: no incluye impuestos, bonos, descuentos ni reglas legales.

La pantalla de marcación pide ubicación y una baliza BLE cercana antes de habilitar el botón.
Además compara la lectura del GPS con el centro de la faena y no registra si el trabajador
está fuera del radio configurado. Si falta algo, explica qué pasa y no guarda nada. Cada marca
nace como pendiente de sincronización y la interfaz lo dice así, porque todavía no hay
servidor que la valide.

## Cuentas de demostración

| Rol | Correo | Contraseña |
|---|---|---|
| Obrero | `obrero@smartvolt.cl` | `demo1234` |
| Capataz | `capataz@smartvolt.cl` | `demo1234` |
| Administrador | `admin@smartvolt.cl` | `demo1234` |

## Almacenamiento y datos

La asistencia, las solicitudes y la faena viven en Room, base `smartvolt.db`, y sobreviven al
cierre de la aplicación. La base está en la versión 1 y se crea desde cero, sin migraciones.
Tiene tres tablas:

- `asistencias`: marcas de entrada y salida con GPS, baliza y estado de sincronización.
- `faenas`: nombre, dirección, centro, radio, tarifa por hora y recargo de horas extra.
- `solicitudes_horas_extra`: solicitudes y decisiones del administrador.

Cada ViewModel abre la base con `AppDatabase.obtener(getApplication())` y usa directamente los
DAO. La faena parte con 7.000 pesos por hora y 50 por ciento de recargo, y esos valores se
escriben recién en el primer guardado.

Las cuentas son ficticias. La autenticación de demostración no representa un sistema real y
no se deben cargar datos personales, credenciales ni llaves privadas.

## Exportación del registro

El administrador puede guardar el mes en dos formatos, ambos con el selector de archivos de
Android y sin permisos de almacenamiento:

- CSV con nombre `registro-smartvolt-AAAA-MM.csv`, en UTF-8 con BOM, separador punto y coma
  y coma decimal. Tiene una fila por trabajador y día, con 12 columnas que incluyen horas de
  asistencia, horas extra, montos por separado y monto total. La última fila es el total del
  mes.
- PDF con nombre `registro-smartvolt-AAAA-MM.pdf`, en hoja A4 vertical. Lleva el resumen del
  mes, la tabla por trabajador y día, y el total. Si la tabla no cabe, agrega páginas con el
  encabezado repetido y el número de página al pie.

## Cómo ejecutar

1. Clonar el repositorio y abrirlo en Android Studio.
2. Esperar la sincronización de Gradle y aceptar el SDK que pida.
3. Elegir la configuración `app` y un emulador o teléfono conectado.
4. Ejecutar con el botón Run.

Desde PowerShell:

```powershell
.\gradlew.bat assembleDebug        # compilar
.\gradlew.bat testDebugUnitTest    # 23 pruebas unitarias
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

## Documentación

- [docs/pruebas-area-c.md](docs/pruebas-area-c.md): pruebas de campo, contratos de ubicación
  y BLE, umbrales y casos PT.
- [docs/faena-configuracion.md](docs/faena-configuracion.md): campos de la faena, cálculo del
  radio y alcance.
- [docs/registro-mensual.md](docs/registro-mensual.md): cálculo de horas y montos, formato del
  CSV y estructura del PDF.
- [docs/DISENO_VISUAL_SMARTVOLT.md](docs/DISENO_VISUAL_SMARTVOLT.md): paleta, tipografía y
  criterios visuales.
