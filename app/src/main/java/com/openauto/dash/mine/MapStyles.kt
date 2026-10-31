// MINE: los estilos de OpenFreeMap que el conductor elige de una lista (#19),
// en lugar de pegar una URL a mano en Servidores de mapa y rutas. Los nombres
// son los que usa OpenFreeMap para cada estilo; la URL es la del estilo tal
// cual, servida por su servidor público.
package com.openauto.dash.mine

/** One basemap the driver can pick: the name OpenFreeMap gives it and the style URL behind it. */
internal class MapStyle(val name: String, val url: String)

internal object MapStyles {
    private const val BASE = "https://tiles.openfreemap.org/styles"

    val positron = MapStyle("Positron", "$BASE/positron")
    val bright = MapStyle("Bright", "$BASE/bright")
    val liberty = MapStyle("Liberty", "$BASE/liberty")
    val dark = MapStyle("Dark", "$BASE/dark")
    val fiord = MapStyle("Fiord", "$BASE/fiord")

    /** What the light slot offers, in the order the driver sees it. */
    val lightChoices = listOf(positron, bright, liberty)

    /** What the dark slot offers. */
    val darkChoices = listOf(dark, fiord)

    /** Every style we know, to match a URL kept by hand. */
    private val all = lightChoices + darkChoices

    /** The style behind [url], or null when it is a URL from somewhere else (the driver's own). */
    fun of(url: String): MapStyle? {
        val kept = url.trim()
        return all.firstOrNull { it.url == kept }
    }
}
