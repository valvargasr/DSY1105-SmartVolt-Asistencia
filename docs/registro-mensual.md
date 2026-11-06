# Registro mensual

El administrador puede consultar las horas trabajadas en el mes y guardar el detalle en CSV o
PDF. La entrada está en Gestión, en la tarjeta "Registro del mes", y también en el acceso del
inicio. Solo la ve ese rol.

## Qué muestra

La pantalla abre en el mes actual y permite retroceder con las flechas. No deja avanzar más
allá del mes en curso porque no existen marcas futuras.

El resumen de la faena incluye el total de horas, las horas de asistencia, las horas extra
aprobadas, la cantidad de trabajadores, la cantidad de días con marcas y las solicitudes
pendientes.

Si un trabajador quedó con una entrada sin salida, aparece un aviso y esas horas no se suman.

Debajo se listan los trabajadores con su total de horas, el desglose de asistencia y horas
extra, y una fila por día con el horario registrado y las horas de ese día.

## Cálculo de horas

El detalle por día se obtiene así:

1. Se ordenan las marcas del día por hora.
2. Cada entrada abre un tramo y la salida siguiente lo cierra.
3. Los tramos cerrados se suman en minutos.
4. Si el día termina con una entrada abierta, el tramo no se suma y el día queda marcado como
   sin salida.
5. Las horas extra aprobadas del día se suman aparte, usando la hora autorizada de inicio y
   de término.

Las solicitudes pendientes o rechazadas se cuentan en el resumen, pero no suman horas. La
autorización de una solicitud no demuestra que esas horas se hayan trabajado, tal como
advierte el modelo `SolicitudHoraExtra`.

Las marcas se agrupan por fecha local del teléfono. Un turno que cruza la medianoche queda
partido en dos días.

## Archivo CSV

El botón "Guardar registro en CSV" abre el selector de archivos de Android con el nombre
`registro-smartvolt-AAAA-MM.csv`. No se piden permisos de almacenamiento.

El archivo usa UTF-8 con BOM, separador punto y coma y coma decimal, para que Excel en
español lo abra sin pasos extra. Tiene una fila por trabajador y día, con estas columnas:

| Columna | Contenido |
|---|---|
| Fecha | Fecha ISO, por ejemplo 2026-11-03 |
| Trabajador | Nombre de la cuenta demo |
| Identificador | Código del trabajador |
| Entrada | Hora de la primera entrada o vacío |
| Salida | Hora de la última salida o vacío |
| Horas asistencia | Horas decimales del día |
| Sin salida | Sí o No |
| Horas extra aprobadas | Horas decimales del día |
| Horas totales | Suma de asistencia y extra del día |

La última fila lleva la etiqueta TOTAL con las horas del mes.

## Archivo PDF

El botón "Guardar registro en PDF" genera `registro-smartvolt-AAAA-MM.pdf` y lo entrega al
mismo selector de archivos, sin permisos de almacenamiento.

El documento usa una hoja A4 vertical. La primera página lleva el nombre de la faena, el mes,
la fecha de generación y un resumen con trabajadores, días y horas. Después viene una tabla
por trabajador y día con la fecha, la entrada, la salida, las horas y las horas extra. Al
final aparece la fila con el total del mes.

Cuando la tabla no cabe en la hoja, el documento agrega páginas y repite el encabezado y la
fila de columnas. Cada página muestra su número al pie.

## Límites

El registro se calcula solo con lo que está guardado en el teléfono. No hay backend, así que
no incluye marcas de otros dispositivos ni sincronización pendiente. Tampoco considera
feriados, turnos teóricos ni atrasos: para eso haría falta definir esas reglas primero.

El registro es solo de horas.

## Cómo probarlo

1. Entrar como administrador con `admin@smartvolt.cl` y la contraseña `demo1234`.
2. Volver al inicio y abrir "Registro del mes".
3. Cambiar de mes con las flechas y volver al mes actual.
4. Revisar las horas de asistencia, las horas extra y el total del mes.
5. Tocar "Guardar registro en CSV", elegir una carpeta y abrir el archivo.
6. Repetir con "Guardar registro en PDF" y revisar la tabla y el total.
7. Comparar la fila TOTAL con la suma de las filas.
