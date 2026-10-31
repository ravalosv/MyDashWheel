// MINE: más de una ruta a un destino, y la que el conductor elige (#25). Valhalla
// solo calcula alternativas si se le piden, que es una clave más del cuerpo de la
// petición; pedirla es lo que vive aquí, y el archivo heredado solo llama a [body]
// al armar la suya. Elegir cuál se sigue también es nuestro: la librería marca la
// elegida en el mapa, pero no dice cuál es, y lo único que distingue una ruta de
// otra es su geometría.
package com.openauto.dash.mine

import org.json.JSONObject

internal object AlternativeRoutes {
    /** Las alternativas que se piden además de la principal: tres líneas caben en la pantalla. */
    const val WANTED = 2

    /** La geometría de la ruta que el conductor marcó en el mapa; null, ninguna a mano. */
    @Volatile
    private var chosenGeometry: String? = null

    /** La librería avisa de la ruta marcada ([OnRouteSelectionChangeListener]): esta es la que se guía. */
    fun chosen(geometry: String?) {
        chosenGeometry = geometry
    }

    /** Ruta nueva, o cerrada: vuelve a decidir el servidor con su primera. */
    fun forget() {
        chosenGeometry = null
    }

    /** La geometría elegida, si sigue viva la búsqueda que la eligió. */
    fun chosenGeometry(): String? = chosenGeometry

    /**
     * [json], el cuerpo de la petición ya armado, con `alternates` cuando está
     * encendida la opción. A partir de dos, el servidor devuelve la principal y
     * las alternativas que encuentre; en un viaje largo a veces devuelve una
     * sola, y la ruta sigue siendo buena. Apagada, se deja el cuerpo tal cual:
     * no se pide nada de más.
     */
    internal fun body(json: String, enabled: Boolean): String {
        if (!enabled) return json
        return runCatching {
            val o = JSONObject(json)
            if (!o.has("alternates")) o.put("alternates", WANTED)
            o.toString()
        }.getOrDefault(json)
    }

    /**
     * Qué ruta de [geometries] es la que se sigue: la elegida ([chosen]) si
     * sigue en la lista, y si no —nunca elegida, o elegida una búsqueda que ya
     * no es esta— la primera, que es la que el servidor recomienda. Devuelve el
     * índice, o -1 cuando no hay ninguna.
     */
    internal fun indexOf(chosen: String?, geometries: List<String?>): Int {
        if (geometries.isEmpty()) return -1
        if (chosen == null) return 0
        val at = geometries.indexOf(chosen)
        return if (at >= 0) at else 0
    }
}
