# Plan: hoja de ajustes del mapa y capas

**Fecha:** 2026-10-30
**Estado:** Autorizada (el propietario pide continuar con el #16 tras cerrar el #15)

**GitHub Ticket:** [#16](https://github.com/ravalosv/MyDashWheel/issues/16) en `ravalosv/MyDashWheel`

---

## Contexto

El mapa (`MapLibrePanel`) sirve a tres sitios: el tile de Mapa, el fondo del tema **Canvas**
(`wallpaper = true`) y el cluster. Casi todo está fijo en el código: la cámara del fondo (zoom 17,
inclinación 60°, coche al 42 %), los gestos apagados y las capas (solo edificios 3D). El propietario
quiere ajustarlo sin tocar código y poder añadir capas.

Decisión del propietario (2026-10-30): **no** se trabaja la coloración del mapa; sí las capas.
*(Cambiada el 2026-10-31: sí se trabaja el estilo —siempre oscuro / claro / según el tema, #18— y
cuál de los estilos de OpenFreeMap carga cada uno, #19. Sigue sin tocarse un color del estilo a mano.)*

**Alcance corregido (2026-10-30, el propietario):** todo lo pedido se enfoca al **widget de mapa**
(`MapLibrePanel(wallpaper = false)`), que es el mapa que el propietario usa. El Canvas deja de ser su
tema principal: el fondo del Canvas puede seguir aceptando lo mismo, pero ni se depura ni se toma
como referencia. Las decisiones de cámara (zoom, inclinación, altura del coche) se retiran: la cámara
del widget la manda el seguimiento heredado del coche, y pisarla desde fuera pelea con él.

## Incrementos

1. **Base** — `MapOptions`, `MapOptionsStore`, `MapLook`, hoja «Mapa» en Ajustes → Aspecto y el
   enganche en `MapLibrePanel`: gestos y brújula del widget ya se aplican (la brújula es nuestra,
   `MapCompass`, porque la de MapLibre solo aparece con el mapa girado). *(entregado)*
2. **Capas** — `MapLayers` (fuente + capa re-añadidas tras cada `setStyle`) y la primera capa sin
   llave: obras y cierres por Overpass. *(la infraestructura se entrega en el 3; Overpass queda aquí)*
3. **Tráfico** — TomTom Flow & Incidents. *(este entrega)* La clave **no** va en `ApiService`: ese
   enum es heredado y sus dos `when` habría que editar. Vive en sus propias prefs
   (`MapTraffic.key`), con su campo en la hoja del mapa, que es lo mismo para el conductor: un sitio
   donde pegarla y dónde conseguirla. Si más adelante se quiere en Ajustes → Expert → Claves, se
   añade entonces con su línea `// MINE: #16`.

   **La clave, en Experto → Tus claves API (2026-10-31, el propietario).** Ticket
   [#20](https://github.com/ravalosv/MyDashWheel/issues/20). Al final no va en la hoja del mapa: las
   claves van todas juntas, donde ya están la de Gemini y la de Tankerkönig, con el ojo para mostrarla
   y el QR de dónde conseguirla. `ApiService` gana la entrada `TRAFFIC`, que **no** cambia de almacén
   —sigue en las prefs de `MapTraffic`—: en `ApiKeys.kt` solo entran tres líneas marcadas `// MINE:
   #20` (el `import`, la rama del `get` y la del `set`). El campo de la hoja del mapa se retira, y su
   «Restablecer» deja de borrar la clave, que ya no es suya. Como las capas leen
   `MapTraffic.key(context)` cada segundo, los incidentes salen sin reiniciar la app.
   MapLibre 11.11 no tiene listener de estilo cargado (`addOnStyleLoadedListener` no existe en esta
   versión), así que `MapLayers` vigila la identidad de `map.style` una vez por segundo y viste cada
   estilo nuevo: así el tráfico sobrevive al cambio día/noche.

   **Tráfico sobre la ruta (añadido 2026-10-30, tras probarlo el propietario).** Las teselas de flujo
   de TomTom son de la ciudad entera: con una ruta trazada se veía el tráfico de todas las calles y no
   el de la ruta, que es lo que se ve en Google Maps. Ahora, con ruta, el flujo de ciudad se retira y
   el color va **sobre la línea**: `RouteFeed` publica la ruta que el mapa está dibujando (la de la
   guía de la app, la de la vista previa o la que se sigue tras entregar el destino a Maps/Waze),
   `MapRouteTraffic` pide **una** ruta a TomTom entre los mismos dos extremos con `sectionType=traffic`
   —se repite cada 3 min, que el tráfico cambia y la línea no— y `RouteBands` pega cada tramo
   congestionado a nuestra línea (puntos proyectados, solo a menos de 40 m; un tramo que se va por otra
   calle se corta en dos). Se pinta una capa por grado de TomTom (ámbar, naranja, rojo, granate) **encima**
   de la línea de ruta de la librería, repuesta cada segundo por si ella la vuelve a dibujar. Coste:
   una petición del Routing API por ruta y refresco (la clave es la misma; la cuota gratuita de un
   coche lo cubre); un rechazo se reintenta a los 20 s.

   **Sin ruta no hay tráfico (2026-10-30, el propietario).** El flujo de ciudad de TomTom se retira
   del todo: al arrancar el mapa se encendía la ciudad entera, que es justo lo que no quiere. Sin
   ruta trazada no se pinta ninguna capa de flujo y **no se pide nada** del Routing: la cuota solo se
   gasta mientras se guía. Si la petición de la ruta falla, no hay bandas: el mapa queda limpio, nunca
   con colores de la ciudad.

   **Los incidentes, siempre (2026-10-30, el propietario).** Corrección a lo anterior: la capa de
   **incidentes** de TomTom (obras, cortes, accidentes) se pinta siempre que haya clave y la opción de
   tráfico esté encendida, **con ruta o sin ella** — un accidente delante del coche se quiere ver
   aunque no se vaya a ningún sitio. Es una tesela raster suya, no el flujo: no añade peticiones por
   ruta, solo las del mapa que se está mirando. Lo que sigue necesitando ruta es el color sobre la
   línea.

4. **Radar** — RainViewer (licencia personal, que cubre a esta app).
5. **Satélite y relieve** — fuente raster y raster-dem con hillshade.
6. **Ruta** — color y grosor (`RouteLineOptions`), modo vista parado y rutas alternativas.
7. **Textos** en `strings_mine.xml` (es/en) de lo que falte.

**Estilos de OpenFreeMap de una lista (2026-10-31, el propietario).** Ticket
[#19](https://github.com/ravalosv/MyDashWheel/issues/19). Hasta ahora el estilo se cambiaba pegando una
URL en la hoja heredada «Servidores de mapa y rutas»; el conductor lo quiere **elegir**. `mine/MapStyles.kt`
guarda los cinco estilos con su nombre y su URL (`Positron`, `Bright`, `Liberty`, `Dark`, `Fiord`) y
las dos listas: **claro → Positron, Bright, Liberty**; **oscuro → Dark, Fiord**. La sección «Estilos de
mapa» de la hoja son dos filas de píldoras (reutilizan la píldora del estilo del mapa, #18) y el
elegido se guarda donde el mapa ya lo lee, `MapServers` — sin duplicar el dato y sin tocar el archivo
heredado. Si la URL guardada no es de la lista, ninguna píldora sale marcada y la propia se respeta; el
campo de URL sigue vivo en la hoja de servidores.

**Estilo del mapa (2026-10-31, el propietario).** Ticket
[#18](https://github.com/ravalosv/MyDashWheel/issues/18). El widget elige hoy su estilo con el tema
del panel (`DashColors.Light` → `styleDark` / `styleLight`, en `MapLibrePanel.kt`); el conductor
quiere poder clavarlo: **siempre oscuro**, **siempre claro** o **según el tema** (lo de siempre, y
el valor por defecto, así que sin tocarlo nada cambia). `MapOptions.theme` es un enum `MapTheme` con
`isLight(panelLight)`, que es toda la decisión y lo que se prueba; se guarda con las demás opciones y
un nombre que ya no exista cae en `THEME`. En la hoja es una fila de tres píldoras, porque tres
interruptores no dicen «una de estas». El **fondo de Canvas sigue al tema**: se decide en la misma
línea del enganche (`if (wallpaper) …`), y ese mapa no es el del conductor. Al cambiar el estilo el
`LaunchedEffect(mapRef, styleUrl)` heredado hace el `setStyle` de siempre y `MapLayers` vuelve a
vestir el estilo nuevo como ya hacía en el cambio día/noche.

**El mapa, al final de Aspecto y con encabezado propio (2026-10-31, el propietario).** Ticket
[#21](https://github.com/ravalosv/MyDashWheel/issues/21). La fila «Mapa» estaba en mitad del panel,
entre el día/noche y la animación de inicio, y el mapa no es parte del aspecto del panel: pasa al
final del listado, **debajo de «Animación de Inicio»**, precedida de su propia sección
(`mine_map_section`) para que se distinga. Solo cambia el orden en `LookPane` (`SettingsScreen.kt`) y
la cadena del encabezado; cada línea tocada va marcada `// MINE: #21`.

**Ver la ruta entera (2026-10-31, el propietario).** Ticket
[#23](https://github.com/ravalosv/MyDashWheel/issues/23). Con una ruta en curso el conductor quiere
verla **completa** de un vistazo. Sobre el widget, encima de «Centrar», aparece un segundo redondo de
44.dp —solo icono, como el del #22, con la cadena `mine_route_overview` como descripción— **solo
mientras `RouteFeed.route` no sea null**: la misma línea que ya pinta el tráfico, así que sirve para la
guía, la vista previa y la ruta entregada a Maps/Waze. Al pulsarlo, `MapRouteOverview` calcula la caja
que abarca todos los puntos (`RouteBox`, pura y probada) y encuadra sus dos esquinas con 48 px de
margen, **sin inclinación ni giro**. Es una cámara nuestra, no del componente: deja de seguir al coche
y sale el botón «Centrar» para volver.

*Corregido el 2026-10-31, tras probarlo el propietario (el botón no encuadraba y el mapa seguía
inclinado):* dos cosas faltaban. **El componente seguía siguiendo al coche**, así que su siguiente
posición devolvía la cámara al coche en mitad de la animación: ahora `fit` llama primero a
`MapRecentering.stopFollowing` —que guarda la `LocationComponent` de cada mapa desde el #17 y le pone
`CameraMode.NONE`—. Y **`newLatLngBounds` conserva inclinación y rumbo** que encuentra: ahora se pide
la cámara con `getCameraForLatLngBounds(bounds, padding)`, que da destino y zoom, y se anima con
`CameraPosition` propia a `tilt = 0`, `bearing = 0`.

*Segunda corrección (2026-10-31, segunda prueba del propietario: «a veces sí, a veces no; al segundo
toque sí; en guía hace zoom pero no centra»).* Faltaba parar las cámaras que ya iban en marcha: la
animación de llegada de la ruta (el upstream vuela al destino al recibirla) y la transición de 1 s que
«Iniciar» arranca con `followVehicle`. Se colaban encima de la nuestra y dejaban el mapa alejado pero
sobre el coche. Ahora `fit` llama a `map.cancelTransitions()` antes de moverse, anima en 700 ms y
**comprueba al reposar** (un `OnCameraIdleListener` de un solo uso): si el destino o el zoom no son los
pedidos (200 m o 0,4 de zoom de tolerancia), lo vuelve a poner una vez. Así el primer toque basta.

*Tercera corrección (2026-10-31, tercera prueba del propietario: «sigue sin funcionar; necesita dos
toques, esperando ya quieta la animación»).* No era una carrera: era la propia consulta del encuadre.
`getCameraForLatLngBounds(bounds, padding)` **no** calcula para un mapa plano —así lo dice el código de
MapLibre— sino para la cámara que encuentra (`transform.getRawBearing()`, `transform.getTilt()`). Con el
mapa aún inclinado 45° y girado con el coche, el primer toque pedía un destino y un zoom válidos para
*esa* vista y los aplicaba planos: bajaba el zoom sí, pero descentrado. El segundo toque acertaba porque
ya se encontraba el mapa plano: el primero había servido, sin querer, de aplanado. Ahora se pide el
encuadre con la sobrecarga de cuatro argumentos, `getCameraForLatLngBounds(corners, padding, 0.0,
0.0)`, es decir con la inclinación y el rumbo **de destino**, con lo que el primer toque ya encaja.

**Rutas alternativas (2026-10-31, el propietario).** Ticket
[#25](https://github.com/ravalosv/MyDashWheel/issues/25). Fila «Rutas alternativas» del diseño, la que
faltaba de «En la ruta». Valhalla calcula alternativas solo si se le piden: es la clave `alternates`
del cuerpo. Probado contra el servidor por defecto (`valhalla1.openstreetmap.de`, Valhalla 3.9.1) con
`format=osrm`: `alternates: 2` devuelve tres rutas en trayectos de ciudad, una sola en un viaje largo
(Guadalajara–Puerto Vallarta) — el servidor no encuentra otra y la ruta sigue siendo válida.

`mine/AlternativeRoutes.kt` guarda lo puro: `body` añade `alternates` al cuerpo ya armado cuando la
opción está encendida (apagada, el cuerpo sale tal cual; una sola petición, ni deformada ni repetida) e
`indexOf` decide cuál de las recibidas se sigue —la elegida por su geometría, y si ya no está, la
primera del servidor—. La opción es `MapOptions.alternatives`, **encendida por defecto**, con su
`SettingsToggle` en la sección «Capas». En `MapLibrePanel.kt` hay cuatro líneas `// MINE: #25`: el
cuerpo de `valhallaRoute`, la elección de `resp.routes[index]` (antes siempre `first()`), el
`setOnRouteSelectionChangeListener` del `NavigationMapRoute` —que apunta la geometría tocada y deja esa
ruta como la que se guía— y el `forget()` de `clearRoute`.

*Corregido el 2026-10-31 tras probarlo el propietario («al seleccionar otra ruta, no la marca para
seguirla»):* `NavigationMapRoute` **solo registra su listener de toque en `onStart()`** —su constructor
no lo añade, lo hace `addListeners()`—, y el código heredado crea el objeto y nunca llama a `onStart()`.
Así que el toque sobre una alternativa no llegaba a nadie: ni marcaba ni avisaba. Se llama `onStart()`
justo después de crearlo (con el mapa ya arriba); `onStop()` no hace falta porque el objeto vive lo que
vive el estilo y no se retira nunca.

*Segunda corrección del mismo día (el propietario: «le doy toque sobre la principal o la secundaria y no
hace nada en ningún caso»).* `clearRoute` llama a `navRoute.removeRoute()`, que en la librería **vacía
también el mapa `LineString → ruta`** del que el listener saca a qué ruta corresponde el toque
(`MapRouteLine.clearRouteListData`). Con ese mapa vacío, `invalidMapClick` corta en seco y la lista
**no se vuelve a rellenar al dibujar rutas nuevas** (solo se llena en el constructor de `MapRouteLine`
y en el callback de `FeatureProcessingTask`; un `addRoutes` posterior no lo toca, porque
`featureProcessingTaskRef` sigue apuntando a la tarea anterior). En la vista previa el flujo siempre es
`clearRoute()` → `routeTo()`, así que el toque quedaba mudo para siempre. Ahora `clearRoute` termina con
`navRoute.addRoutes(emptyList())`, que dibuja una lista vacía y deja el listener con datos frescos para
la ruta siguiente. También se quitó el `forget()` que se había puesto en `routeTo`: borraba la elección
que el toque acababa de guardar. Y al elegir, la línea de tiempo/distancia pasa a ser la de la ruta
elegida.

## Aislamiento (regla de desacoplamiento del fork)

| Pieza | Dónde |
|---|---|
| Opciones y su almacén | `app/src/main/java/com/openauto/dash/mine/MapOptions.kt` — nuevo |
| Aplicarlas al mapa | `mine/MapLook.kt` — nuevo |
| La hoja | `mine/MapSettingsSheet.kt` — nuevo |
| Capas | `mine/MapLayers.kt` — nuevo |
| Tráfico de la ciudad | `mine/MapTraffic.kt` — nuevo (tras la corrección: solo incidentes, sin flujo) |
| Ruta que se dibuja | `mine/RouteFeed.kt` — nuevo |
| Tráfico sobre la ruta | `mine/MapRouteTraffic.kt` y `mine/RouteBands.kt` — nuevos |
| Volver al coche (#17) | `mine/MapRecentering.kt` y `mine/MapCenterButton.kt` — nuevos; el botón, solo icono desde el #22 |
| Rutas alternativas (#25) | `mine/AlternativeRoutes.kt` (el cuerpo y la elección) — nuevo; la opción en `mine/MapOptions.kt` y su fila en `mine/MapSettingsSheet.kt`; cuatro líneas `// MINE: #25` en `MapLibrePanel.kt`; cadenas `mine_map_alternatives`/`_detail` en `strings_mine.xml` (es/en) |
| Ruta completa (#23) | `mine/MapRouteOverview.kt` (`RouteBox`, el encuadre) y `mine/MapRouteOverviewButton.kt` — nuevos; una línea `// MINE: #23` en `MapLibrePanel.kt`; cadena `mine_route_overview` en `strings_mine.xml` (es/en) |
| Estilo del mapa (#18) | `mine/MapOptions.kt` (enum `MapTheme`), `mine/MapLook.kt` (`mapIsLight`), `mine/MapSettingsSheet.kt` (la fila) — y una línea `// MINE: #18` en `MapLibrePanel.kt` |
| Estilos de OpenFreeMap (#19) | `mine/MapStyles.kt` (las listas) y la sección «Estilos de mapa» de `mine/MapSettingsSheet.kt`; escribe en `MapServers` (heredado), sin tocar sus archivos |
| Clave de TomTom (#20) | `mine/MapTraffic.kt` (su almacén, sin cambios) y tres líneas `// MINE: #20` en `ApiKeys.kt`, el archivo heredado de las claves |
| Su sitio en Aspecto (#21) | `SettingsScreen.kt`, `LookPane`: la fila al final, tras «Animación de Inicio», y su `SettingsSection`; cadena `mine_map_section` en `strings_mine.xml` (es/en) |
| Enganches | `SettingsScreen.kt`: entrada `Deep.MAP`, su rama `when`, la fila en `LookPane` y su firma; `MapLibrePanel.kt`: una llamada a `MapLook.watch`, la brújula sobre el widget, el botón «Centrar», una llamada a `MapRecentering.watch` en lugar del temporizador de 10 s y dos líneas que avisan a `RouteFeed` de la ruta de vista previa (al dibujarla y al retirarla). Todas `// MINE:` con su ticket |

## Criterios de aceptación

- [x] Hoja «Mapa» en Ajustes → Aspecto, con las opciones y sus valores guardados entre arranques.
- [x] Con todo por defecto, el widget se comporta como antes (se puede tocar; la cámara sigue al
      coche con `followVehicle` y, desde el #17, el botón «Centrar» cuando se suelta).
- [x] La brújula se ve sobre el widget siempre, no solo con el mapa girado.
- [x] El botón de volver al coche es solo el icono, un redondo del tamaño de la brújula, sin texto
      visible (#22).
- [x] Con ruta en curso hay un botón «Ruta completa» —solo icono— que encuadra la línea entera, sin
      inclinación; sin ruta no aparece. Tests de `MapRouteOverview`. Probado por el propietario el
      2026-10-31 tras la tercera corrección: encuadra al primer toque, también en guía y repetido
      tras «Centrar».
- [x] Tests del modelo de opciones.
- [x] Con una ruta trazada el tráfico se pinta **sobre la ruta**; **sin ruta no hay color ni petición
      del Routing**. Los incidentes se ven siempre. Tests de `RouteBands` (tramo pegado, tramo en otra
      calle, tramo que se va y vuelve, grado 0, lectura de `sections` de TomTom) y de `MapRecentering`.
- [x] El estilo del mapa se puede clavar en oscuro o claro, independiente del tema del panel, y por
      defecto sigue al tema. El fondo de Canvas no cambia. Tests de `MapTheme`.
- [x] El estilo de mapa se elige de una lista (claro: Positron, Bright, Liberty; oscuro: Dark, Fiord),
      el elegido sale marcado, y una URL propia guardada se respeta sin marcar ninguna. Tests de
      `MapStyles`.
- [x] La clave de TomTom se pega en Ajustes → Experto → Tus claves API, no en la hoja del mapa.
- [x] En Ajustes → Aspecto, «Mapa» es la última entrada, debajo de «Animación de Inicio» y con su
      propio encabezado.
- [x] `./gradlew :app:compileGithubDebugKotlin` y los tests nuevos.
- [x] `grep -rn "MINE:" app/src/main` encuentra los puntos de contacto.
- [ ] La corrida completa, cuando el propietario pida el commit.

## Riesgos

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| `MapLook` pisa la cámara del widget | Bajo | Ya no toca la cámara: solo gestos y brújula |
| Gestos mientras se conduce | Medio | Encendidos por defecto (como antes); apagables, y vuelta al coche con el botón «Centrar» cuando el conductor quiera (ya no hay retorno por temporizador) |
| Las capas se pierden al cambiar de estilo día/noche | Medio | Se re-añaden en el mismo `setStyle` (incremento 2) |

## Volver al coche: botón «Centrar» (2026-10-30, el propietario)

**Ticket:** [#17](https://github.com/ravalosv/MyDashWheel/issues/17) en `ravalosv/MyDashWheel`

Mover el mapa dejaba la cámara fuera del coche y **la devolvía sola a los 10 s**
(`TRACKING_RESUME_MS`). En el coche eso molesta más de lo que ayuda: el conductor mueve el mapa
precisamente cuando quiere mirar otra cosa —una calle, una ruta entera, la dirección que acaba de
buscar— y que se le escape a media consulta es peor que dejarla quieta. El temporizador **se retira**.

En su lugar, mientras la cámara está fuera del coche se muestra un botón **«Centrar»** sobre el
widget (arriba de la brújula). *(Desde el #22 es **solo el icono**, un botón redondo del tamaño de la
brújula; el texto queda como descripción del icono para los lectores de pantalla.)* Al pulsarlo, la cámara vuelve al coche **con el zoom y la inclinación
de conducción** (`followVehicle` con el zoom por defecto: 16.5 en el widget, 17.0 en el Canvas), no
con el zoom que el conductor hubiera puesto a mano —lo que pide es volver a la vista de conducción—.
El botón se va solo en cuanto la cámara vuelve a seguir al coche, venga de donde venga.

`MapRecentering` guarda por mapa dos cosas: si la cámara está fuera del coche (un `StateFlow` que lee
el botón) y qué llamada la devuelve —la que el propio `enableLocation` sabe hacer, con su `lc` y su
`wallpaper`—. Se engancha al `addOnCameraTrackingChangedListener` de la `LocationComponent`, que ya
avisaba cuando se suelta el seguimiento. En `MapLibrePanel.kt` el bloque del temporizador pasa a ser
una línea `// MINE: #17`; fuera también el `Job` y el `delay` que solo servían para él.

Aceptación: arrastrar el mapa y esperar no lo recentra; el botón aparece entonces; al pulsarlo vuelve
al coche con zoom de conducción y desaparece; sin gestos, el comportamiento normal no cambia. Tests de
`MapRecentering`.

