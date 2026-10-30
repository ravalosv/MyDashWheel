# Plan: el destino entregado a Maps o Waze se dibuja también en el mapa propio

**Fecha:** 2026-10-30
**Estado:** Autorizada (el propietario pide implementar la Fase 1 el 2026-10-30)

**GitHub Ticket:** [#13](https://github.com/ravalosv/MyDashWheel/issues/13) en `ravalosv/MyDashWheel`

---

## Contexto

Hay dos sistemas de navegación y no se hablan:

- **`NavHandoff`** (widget Casa/Trabajo, favoritos, el teléfono, voz): abre Google Maps o Waze con
  `google.navigation:` / `waze://` y, antes, llama a `InAppNav.handOver()`.
- **`InAppNav`** (tile de Mapa/Ruta y fondo del tema Canvas): guía dentro de Dashwheel con Valhalla.

`MapLibrePanel` solo dibuja `InAppNav.guidance` (`MapLibrePanel.kt:422-438`). Al tocar Casa se para la
guía interna y no queda nada que dibujar: el tile de mapa y el fondo de Canvas se vacían, aunque la
navegación siga en Maps. `NavHandoff.handedOff` ya existe y lo consumen `ShadowRoute` y
`SpatialSkinTiles`, **nunca el mapa**.

## Decisión

El mapa propio dibuja una **ruta de sombra** al destino del handoff, calculada con el mismo router
gratuito que usa el resto de la app:

1. Se lee `NavHandoff.handedOff` (no hace falta tocar el widget: ya publica cada destino).
2. Se dibuja mientras la guía interna **no** esté activa: cuando lo está, la línea es suya.
3. La ruta se calcula desde la posición del coche con `valhallaRoute` y se **recalcula** si el coche
   se sale de la línea o cuando llega.
4. Nada se incrusta: Google Maps no se puede meter en nuestro layout. Lo que se dibuja es el mapa de
   Dashwheel, no el de Maps.

Esto no cambia quién guía: Maps o Waze siguen dando el giro a giro. Lo que cambia es que el mapa del
tablero y el fondo de Canvas enseñan a dónde va el coche, en vez de quedarse en blanco.

## Aislamiento (regla de desacoplamiento del fork)

| Pieza | Dónde |
|---|---|
| `HandedOffRoute` (regla de qué dibujar, con test) y `HandedOffRouteOnMap` (efecto Compose) | `app/src/main/java/com/openauto/dash/mine/HandedOffRoute.kt` — archivo nuevo |
| Enganche | `MapLibrePanel.kt`: **una línea**, marcada `// MINE: #13` |

`HomeWorkTile.kt` **no se toca**: el widget ya publica el destino en `NavHandoff.handedOff` y el
dibujo lee de ahí, así que el punto de contacto es uno solo.

## Cambios

1. `HandedOffRoute.shown(target, guidingInApp, nowMs)` — función pura: el destino que hay que dibujar,
   o `null`. Descarta el handoff caducado (4 h), el que llega sin posición y sin texto que buscar, y
   todo mientras la guía interna esté activa.
2. `HandedOffRouteOnMap(map, navRoute)` — efecto Compose:
   - resuelve el destino (posición dada, o buscada por sus palabras como ya hace `ShadowRoute`);
   - pone el marcador del destino y `addRoute` en el `NavigationMapRoute` del mapa;
   - sigue el feed de posición: recalcula si el coche va a más de 100 m de la línea durante 10 s
     (con hueco mínimo de 45 s entre peticiones) y borra todo al llegar (80 m del final);
   - al cambiar de destino, al empezar una guía interna o al salir de la pantalla, retira su línea y
     su marcador.
3. `MapLibrePanel.kt`: una llamada a `HandedOffRouteOnMap(mapRef, navRoute)` junto al bloque que ya
   dibuja la guía interna.

## Consecuencias

- El tile de Mapa y el fondo de Canvas se comportan igual: los dos son `MapLibrePanel`.
- Sin handoff, o con la guía interna activa, el comportamiento es exactamente el de hoy.
- Una petición más al router por destino entregado, y hasta una cada 45 s si el coche se sale de la
  línea: el mismo orden que ya hace `ShadowRoute` para la segunda pantalla.
- Un destino escrito en Maps y no en Dashwheel llega **solo** si el servicio de accesibilidad de
  Dashwheel está activo: es la única vía (`MapsScreen` lee su pantalla y llama a `NavHandoff.noticed`,
  `MapsScreen.kt:63-83` y `SplitAccessibilityService.kt:189`). Sin él, el widget de Ruta sigue
  mostrando el giro (lo lee de la notificación, `NavDirections`, que no necesita accesibilidad) pero
  el mapa y el Canvas no tienen nada que dibujar. Comprobado en el aparato el 2026-10-30: con el
  servicio apagado, el logcat no muestra ni una línea de `MapsScreen`.
- Ese caso llega **sin coordenadas**, solo con el texto del destino: se geocodifica con `resolve()`,
  así que su acierto depende del ticket #14. Una dirección que Nominatim no encuentre no se dibuja y
  deja `no position for "…"` en el log.

## Criterios de aceptación

- [ ] Tocar Casa/Trabajo abre la app externa y el tile de Mapa muestra el destino con su ruta.
      Pendiente en el aparato.
- [ ] El fondo del tema Canvas muestra el mismo destino. Pendiente en el aparato.
- [ ] Elegir otro destino reemplaza la línea; llegar a él la borra. Pendiente en el aparato.
- [ ] Con la guía interna activa no aparece ninguna línea de sombra. Pendiente en el aparato.
- [x] Test de la regla `shown()` y de `gap()` (sin destino, guía interna activa, caducado, con
      posición, solo texto): `HandedOffRouteTest`, 7 casos en verde.
- [x] `./gradlew :app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug` en verde
      (13m 11s, `BUILD SUCCESSFUL`).
- [x] `./gradlew :app:lintPlayDebug :app:testPlayDebugUnitTest :app:assemblePlayDebug` en verde
      (11m 59s, `BUILD SUCCESSFUL`).
- [x] `grep -rn "MINE:" app/src/main` encuentra el punto de contacto: `MapLibrePanel.kt:445`.

## Riesgos

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| La línea de sombra y la de la guía interna se pisan | Bajo: dos líneas encima | Se dibuja solo cuando `InAppNav.guidance` es `null` |
| `clearRoute()` del tile borra también nuestro marcador | Bajo: marcador perdido | El efecto se reevalúa al cambiar el destino o la guía y lo vuelve a poner |
| Ruta de sombra distinta de la que sigue Maps | Medio: confusión visual | Es una referencia ("hacia dónde"), no el giro a giro; se recalcula al salir de la línea |
| Un merge del upstream reescribe `MapLibrePanel` y pierde la llamada | Bajo | Marca `// MINE: #13` y `grep` obligatorio tras cada merge |

## Estado de Aprobación

- [x] Autorización para implementar: "implementaremos Fase 1" (2026-10-30)
- [x] Ticket GitHub: [#13](https://github.com/ravalosv/MyDashWheel/issues/13)
