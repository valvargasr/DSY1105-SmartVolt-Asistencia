# Configuración de la faena

La aplicación trabaja con una faena editable: un nombre, una dirección de referencia, un
centro geográfico y un radio en metros. El administrador los cambia desde el inicio, con el
botón de edición de la tarjeta "Faena actual", o desde Gestión, en la tarjeta "Faena". Los
demás roles ven el nombre actualizado, pero no pueden modificarlo.

## Campos y reglas

| Campo | Regla |
|---|---|
| Nombre | Entre 3 y 60 caracteres |
| Dirección | Entre 5 y 120 caracteres |
| Radio | Número entero entre 20 y 1000 metros |
| Latitud | Número entre -90 y 90 |
| Longitud | Número entre -180 y 180 |

Los campos numéricos aceptan coma o punto como separador decimal. Los mensajes de error
aparecen bajo el campo correspondiente y el botón Guardar no envía nada mientras haya un
valor inválido.

El botón "Usar mi ubicación actual" llena la latitud y la longitud con la última lectura del
GPS del teléfono. Si el permiso está denegado, la ubicación está apagada o no hay lectura,
el formulario muestra el motivo y deja los valores como estaban.

## Cálculo del radio

La distancia entre el teléfono y el centro de la faena se calcula con la fórmula de
haversine, usando un radio terrestre de 6.371.000 metros. La marca se permite cuando esa
distancia es menor o igual al radio configurado.

Antes de guardar una entrada o una salida, la aplicación vuelve a leer la faena desde la base
y repite el cálculo. Si el trabajador está fuera, no se escribe nada y el mensaje indica la
distancia al centro y el radio permitido.

## Almacenamiento

La faena se guarda en la tabla `faenas` de la base `smartvolt.db`. La base está en la versión
2 y usa migración destructiva: al actualizar desde una versión anterior se pierden los datos
guardados, algo aceptable para esta demo. Cada guardado sube el campo `perimeterVersion`, que
sirve para distinguir una configuración de otra.

Mientras la tabla está vacía se usa la faena por defecto de la demo:

| Dato | Valor |
|---|---|
| Identificador | FAENA-DEMO-01 |
| Nombre | Faena Andes |
| Dirección | Camino a Farellones km 12, Lo Barnechea |
| Centro | -33.3500, -70.5200 |
| Radio | 300 metros |

Ese valor no se escribe hasta que el administrador guarda por primera vez.

## Alcance

Hay una sola faena y el identificador no cambia al editarla. Por eso las marcas anteriores
siguen visibles en el historial y en el panel, con las coordenadas con las que se
registraron. La aplicación no guarda un historial de perímetros ni permite seleccionar entre
varias faenas. Tampoco bloquea la edición por fecha: el cambio rige desde el momento en que
se guarda.
