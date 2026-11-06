package cl.smartvolt.asistencia.util

import java.util.Locale

object Mapa {

    // Token publico de Mapbox para pedir la imagen del mapa
    private const val TOKEN =
        "pk.eyJ1IjoiZnJlZGNhbXBvczEyMzAiLCJhIjoiY2xudTl2d2VrMDlpbzJrcWpnYnJkc3JqbCJ9" +
            ".hjid1kkpkU37wvVJrj2pQg"

    // Estilo oscuro del mapa
    private const val ESTILO = "mapbox/dark-v11"

    // Arma la URL de un mapa estatico con el pin en la ubicacion
    fun urlEstatica(
        latitude: Double,
        longitude: Double,
        zoom: Int = 16,
        width: Int = 640,
        height: Int = 300
    ): String {
        val lon = "%.5f".format(Locale.ROOT, longitude)
        val lat = "%.5f".format(Locale.ROOT, latitude)
        return "https://api.mapbox.com/styles/v1/$ESTILO/static/" +
            "pin-l+FFC400($lon,$lat)/$lon,$lat,$zoom,0/" +
            "${width}x${height}@2x?access_token=$TOKEN"
    }
}
