package cl.smartvolt.asistencia.native.maps

/**
 * Configuración del mapa estático de Mapbox.
 *
 * Se usa la **Static Images API** con un token público de demostración. No se
 * requiere el token de descarga privado de Gradle (que nunca debe subirse al
 * repositorio), por lo que el proyecto compila sin credenciales secretas.
 *
 * El token es público por diseño de Mapbox y está restringido a la demo
 * académica. Si se publica la app, debe rotarse y restringirse por bundle.
 */
object MapboxConfig {

    const val PUBLIC_TOKEN: String =
        "pk.eyJ1IjoiZnJlZGNhbXBvczEyMzAiLCJhIjoiY2xudTl2d2VrMDlpbzJrcWpnYnJkc3JqbCJ9" +
            ".hjid1kkpkU37wvVJrj2pQg"

    private const val STYLE = "mapbox/dark-v11"

    /** Posición del marcador y centro del mapa en una sola imagen. */
    fun staticMapUrl(
        latitude: Double,
        longitude: Double,
        zoom: Int = 16,
        width: Int = 640,
        height: Int = 300
    ): String {
        val lon = "%.5f".format(java.util.Locale.ROOT, longitude)
        val lat = "%.5f".format(java.util.Locale.ROOT, latitude)
        return "https://api.mapbox.com/styles/v1/$STYLE/static/" +
            "pin-l+FFC400($lon,$lat)/$lon,$lat,$zoom,0/" +
            "${width}x${height}@2x?access_token=$PUBLIC_TOKEN"
    }
}
