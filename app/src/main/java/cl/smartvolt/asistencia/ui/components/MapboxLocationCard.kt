package cl.smartvolt.asistencia.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.smartvolt.asistencia.native.maps.MapboxConfig
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mapa estático de Mapbox con la posición del marcaje.
 *
 * Es una integración real con la API de Mapbox (Static Images): descarga el
 * mapa por HTTPS, lo cachea en memoria y lo muestra dentro de la tarjeta de
 * ubicación. Si no hay red, informa el fallo sin bloquear la marcación.
 */
@Composable
fun MapboxLocationCard(
    latitude: Double?,
    longitude: Double?,
    accuracyMeters: Float?,
    modifier: Modifier = Modifier
) {
    var intento by remember { mutableIntStateOf(0) }

    val resultado by produceState<MapLoad>(
        initialValue = MapLoad.Loading,
        latitude,
        longitude,
        intento
    ) {
        if (latitude == null || longitude == null) {
            value = MapLoad.SinUbicacion
            return@produceState
        }
        value = MapLoad.Loading
        value = withContext(Dispatchers.IO) { cargarMapa(latitude, longitude) }
    }

    val pulso = rememberInfiniteTransition(label = "cargaMapa")
    val alphaCarga by pulso.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaCarga"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                when (val estado = resultado) {
                    is MapLoad.Ready -> {
                        Image(
                            bitmap = estado.bitmap.asImageBitmap(),
                            contentDescription = "Mapa de la ubicación del marcaje",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    MapLoad.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(alphaCarga)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cargando mapa de Mapbox...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is MapLoad.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = estado.mensaje,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { intento += 1 }) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reintentar")
                            }
                        }
                    }

                    MapLoad.SinUbicacion -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Verifica la ubicación para ver el mapa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (resultado is MapLoad.Ready && latitude != null && longitude != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = String.format(
                                    Locale.ROOT,
                                    "%.5f, %.5f",
                                    latitude,
                                    longitude
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                            Text(
                                text = accuracyMeters?.let {
                                    "Precisión ±${it.toInt()} m · Mapbox"
                                } ?: "Mapbox",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Mapa satelital del punto de marcaje",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Mapbox",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private sealed interface MapLoad {
    data class Ready(val bitmap: Bitmap) : MapLoad
    data class Error(val mensaje: String) : MapLoad
    data object Loading : MapLoad
    data object SinUbicacion : MapLoad
}

private val cacheMapas = object : LruCache<String, Bitmap>(6) {
    override fun sizeOf(key: String, value: Bitmap): Int = 1
}

private fun cargarMapa(latitude: Double, longitude: Double): MapLoad {
    val key = String.format(Locale.ROOT, "%.4f,%.4f", latitude, longitude)
    cacheMapas.get(key)?.let { return MapLoad.Ready(it) }

    val url = MapboxConfig.staticMapUrl(latitude, longitude)
    var conexion: HttpURLConnection? = null
    return try {
        conexion = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 10_000
            requestMethod = "GET"
            setRequestProperty("Accept", "image/png")
        }
        val codigo = conexion.responseCode
        if (codigo !in 200..299) {
            return MapLoad.Error("Mapbox respondió $codigo. Revisa la conexión.")
        }
        val bitmap = conexion.inputStream.use { BitmapFactory.decodeStream(it) }
        if (bitmap == null) {
            MapLoad.Error("No se pudo leer la imagen del mapa.")
        } else {
            cacheMapas.put(key, bitmap)
            MapLoad.Ready(bitmap)
        }
    } catch (error: Exception) {
        MapLoad.Error("Sin conexión para cargar el mapa.")
    } finally {
        conexion?.disconnect()
    }
}
