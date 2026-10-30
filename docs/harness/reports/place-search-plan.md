# Plan: que la búsqueda de destinos elija la dirección correcta

**Fecha:** 2026-10-30
**Estado:** Autorizada (el propietario pide corregir la búsqueda y añadir los números de casa el 2026-10-30)

**GitHub Ticket:** [#14](https://github.com/ravalosv/MyDashWheel/issues/14) en `ravalosv/MyDashWheel`

---

## Contexto

Al pedir un destino, con frecuencia se rutea a una dirección que no coincide. El recorrido actual de
una búsqueda es:

1. `PlaceSearch.findAll` (`Places.kt:297`) → `search` (`Places.kt:309`): **una** llamada a Nominatim
   con el texto libre en `q=`, `countrycodes=mx` fijo, `limit=5` y un `viewbox` suave de ±0.25°.
2. Sin resultados cerca, otra llamada **sin sesgo**, a nivel nacional (`Places.kt:305`).
3. `MapLibrePanel.searchPlaces` (`MapLibrePanel.kt:254`): con **un solo** resultado, rutea directo.
4. `routeToPlace` (`MapLibrePanel.kt:302`) rechaza todo lo que caiga fuera de los límites de México.

Causas del destino equivocado:

- Nominatim en texto libre está pensado para una dirección completa; "Calle Juárez 45" sin colonia ni
  ciudad devuelve la calle Juárez más importante del país.
- Los nombres de cadena (Soriana, Oxxo, Farmacia Guadalajara) tienen decenas de sucursales y gana la
  que mejor puntúa Nominatim, que no es la más cercana.
- Un falso positivo único se acepta sin confirmación (`MapLibrePanel.kt:270`).
- La lista muestra `display_name` completo, sin nombre ni colonia separados, sin deduplicar.
- Sin `addressdetails` no se sabe si el resultado trae el número de casa escrito.
- México está hardcodeado dos veces; fuera del país la app dice "no encontrado".
- Valhalla devuelve los puntos ajustados en `resp.locations` y se descartan: el marcador no cae en la vía.

## Decisión

Cinco cambios, en este orden, todos sobre el camino de la búsqueda:

1. **Confirmar antes de rutear.** Tras elegir un resultado (o cuando es el único), se geocodifica
   inversamente el punto (`PlaceSearch.nameOf`, `zoom=18`) y se muestra la dirección antes de
   iniciar. El botón de ir lleva esa dirección; no se rutea nada sin un toque.
2. **Consulta contextual.** A `q` se le añade la ciudad/estado actuales cuando lo escrito no los trae,
   y la petición pide `addressdetails=1&namedetails=1&dedupe=1&accept-language=<idioma>&limit=15`.
3. **Lista legible.** Dos líneas por resultado (nombre / colonia-municipio), deduplicado a ~60 m y
   ordenado por distancia al coche.
4. **País real.** `countrycodes` desde `CountryLookup` (que ya existe y cachea por celda de ~10 km) en
   vez de `"mx"`; los límites de México pasan a **aviso** ("destino fuera del país"), no a rechazo.
5. **Números de casa.** Si lo escrito lleva número:
   - y **ningún** resultado trae `house_number` → no se rutea: se avisa de que ese número no se
     encontró en esa calle y se ofrecen las coincidencias sin número, **etiquetadas como tales**;
   - y hay resultados con número **distinto** → se marcan igualmente ("Insurgentes 1200" para "1234");
   - si lo escrito no lleva número, no se bloquea nada: el comportamiento de hoy.

   La detección es pura y testeable: extraer el número del texto (`\\b\\d{1,5}\\b`) y compararlo con el
   `house_number` de cada resultado.

Fuera de esta fase: autocompletado con Photon y ranking por historial de `PlacesStore`.

## Aislamiento (regla de desacoplamiento del fork)

| Pieza | Dónde |
|---|---|
| `PlaceQuery` (texto → consulta contextual, y número pedido) | `app/src/main/java/com/openauto/dash/mine/PlaceQuery.kt` — archivo nuevo |
| `PlaceRanking` (deduplicar, ordenar, marcar los que no tienen el número pedido) | mismo paquete `com.openauto.dash.mine` |
| Textos nuevos | `app/src/main/res/values/strings_mine.xml` y sus traducciones |
| Enganches | `Places.kt` y `MapLibrePanel.kt`: **una línea** cada uno, marcada `// MINE: #14` |

La consulta a Nominatim y el dibujo de la lista siguen siendo del upstream; lo nuestro decide **qué**
se pregunta y **qué** se muestra.

## Criterios de aceptación

- [ ] Un nombre de cadena lista las sucursales más cercanas con su colonia, y no rutea sin elección.
- [ ] Una calle con número inexistente avisa y no rutea.
- [ ] Elegir un destino muestra su dirección inversa antes de iniciar.
- [ ] Fuera de México ya no dice "no encontrado" por los límites fijos.
- [ ] El marcador cae en la vía que devuelve Valhalla.
- [ ] Test de `PlaceQuery`: número extraído, ciudad añadida solo cuando falta, consulta acentos.
- [ ] Test de `PlaceRanking`: deduplicado a 60 m, orden por distancia, marcado de números distintos.
- [ ] `./gradlew :app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug` en verde.
- [ ] `./gradlew :app:lintPlayDebug :app:testPlayDebugUnitTest :app:assemblePlayDebug` en verde.
- [ ] `grep -rn "MINE:" app/src/main` encuentra los puntos de contacto.

## Riesgos

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Añadir la ciudad ensucia una consulta que ya era completa | Medio: menos resultados | Solo se añade si el texto no trae ya ciudad/estado; si la búsqueda contextual no devuelve nada, se repite sin ella |
| Una petición más a Nominatim por búsqueda (contextual + respaldo + inversa) | Bajo: la política pide ≤1/s | Se mantiene una petición por gesto, sin autocompletado en esta fase |
| `CountryLookup` falla (sin red, servidor caído) | Bajo | Cae al comportamiento actual, sin `countrycodes` |
| Más de un texto nuevo en 11 idiomas | Medio: traducciones | Archivo propio por idioma, marcado como nuestro; sin tocar los `strings.xml` heredados |

## Desviaciones al implementar (2026-10-30)

Lo que se hizo distinto de lo escrito arriba, y por qué:

1. **Un solo archivo, no dos.** `PlaceQuery` (consulta) y el ordenado/deduplicado (`ranked`) viven en
   `mine/PlaceQuery.kt`. El "PlaceRanking" aparte era una separación de carpetas sin valor: son 30
   líneas de funciones puras que se usan juntas y se prueban juntas (`PlaceQueryTest`).
2. **Sin añadir ciudad/estado a `q`.** Nominatim pide ≤1 petición por segundo, y averiguar la ciudad
   del coche cuesta otra llamada por búsqueda. La ambigüedad se resuelve igual con el `viewbox` suave
   + `countrycodes` del país real + el orden por distancia al coche, que es lo que faltaba.
3. **Sin geocodificación inversa de confirmación.** El resultado ya trae `display_name` completo; se
   muestra su nombre en la tarjeta de la ruta, junto a los datos, **antes** del botón "Iniciar". No
   hace falta una petición más para enseñar la misma dirección.
4. **La lista se muestra siempre, también con una sola respuesta** (petición del propietario,
   2026-10-30, después de probar el resto en el aparato): se elige tocando una fila, y hasta entonces
   no se rutea nada. Se elimina el ruteo directo del upstream cuando había un único resultado, también
   cuando el número escrito sí estaba en él. `searchAndRoute` (que solo usaba esa misma búsqueda) se
   queda como alias, sin efecto distinto.
5. **Textos en inglés y español solo, y la verificación se ajusta a eso.** `lint` da
   `MissingTranslation` como **error** con los locales que ya existen (corrida del 2026-10-30, las dos
   ediciones en rojo), así que los dos textos nuevos llevan `tools:ignore="MissingTranslation"` en
   `values/strings_mine.xml`, con el `xmlns:tools` declarado solo en nuestro archivo. Decisión del
   propietario (2026-10-30): los otros nueve idiomas no hacen falta. No se desactiva la regla en el
   proyecto entero: los huecos de traducción del upstream se siguen viendo.
6. **La caja alrededor del coche se cierra, y solo después se abre** (informe del propietario,
   2026-10-30: «hacienda la palma» lo mandaba a 30 km con la calle homónima a 5 minutos). El
   `viewbox` heredado **pondera, no filtra**: Nominatim devolvía primero el pueblo de Nuevo León y
   luego Zapotlanejo, nunca la calle de Guadalajara. Ahora la primera consulta lleva `bounded=1` con
   ±0,15° (~15 km); si no hay nada ahí, se repite con la caja suave y el país completo. Medido contra
   Nominatim el 2026-10-30: la consulta cerrada devuelve una sola respuesta, `Calle Hacienda de la
   Palma, Guadalajara`.
7. **Los límites de México se eliminan, no se convierten en aviso.** Mantener el aviso exigía saber el
   país del destino (otra petición) o enseñar un aviso que la tarjeta tapa con la ETA. `routeTo` ya
   rechaza destinos a más de 5000 km, que cubre el caso absurdo.

## Estado de Aprobación

- [x] Autorización para implementar: el propietario pide incluirlo en la solución (2026-10-30)
- [x] Ticket GitHub: [#14](https://github.com/ravalosv/MyDashWheel/issues/14)
- [x] Implementación: en curso en `feat/#14-place-search` (código y tests escritos; pendiente compilar,
  probar en el aparato y autorizar el commit)
