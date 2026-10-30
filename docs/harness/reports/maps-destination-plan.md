# Plan: leer el destino de una ruta iniciada dentro de Google Maps

**Fecha:** 2026-10-30
**Estado:** Autorizada (el propietario pide continuar con el #15 tras dar por bueno el #14)

**GitHub Ticket:** [#15](https://github.com/ravalosv/MyDashWheel/issues/15) en `ravalosv/MyDashWheel`

---

## Contexto

Una ruta iniciada **dentro de Google Maps** no llega a Dashwheel con destino: el widget de turnos lo
lee de la notificación (`NavDirections`), pero el mapa propio y el fondo de Canvas no tienen qué
dibujar. El #13 preparó el dibujo ([`HandedOffRoute`](../../../app/src/main/java/com/openauto/dash/mine/HandedOffRoute.kt));
lo que falta es **el dato**: `MapsScreen.destinationOf` exige una etiqueta «Tu ubicación» que esta
versión de Maps en español no expone (logcat del 2026-10-30, ver el ticket).

El destino **sí** está expuesto en la tarjeta del lugar, antes de pulsar «Iniciar»:

```
Cómo llegar a Gran Terraza Oblatos, Av. Circunvalación Oblatos 2700
```

## Decisión

1. **Leer la tarjeta**: la primera descripción de la pantalla que empiece por el prefijo de Maps
   («Cómo llegar a », «Directions to », «Itinéraire vers »…) da el resto como destino completo (nombre
   y dirección, tal cual, que es lo que el #14 sabe buscar).
2. **Recordarla, no entregarla al verla**: el conductor puede abrir la tarjeta sin ir a ningún sitio.
   Se guarda durante dos minutos y se entrega **cuando Maps guía de verdad** (`NavDirections.running`
   no nulo), una sola vez por destino.
3. **No pisar la heurística heredada**: si `destinationOf` ya encontró el destino, no se usa la
   tarjeta. La heurística anterior sigue siendo el camino principal donde funciona.

## Aislamiento (regla de desacoplamiento del fork)

| Pieza | Dónde |
|---|---|
| Lectura de la tarjeta y memoria del pendiente (`MapsDestination`, `MapsCard`) | `app/src/main/java/com/openauto/dash/mine/MapsDestination.kt` — archivo nuevo |
| Enganche | **una línea** en `MapsScreen.kt`, marcada `// MINE: #15` |
| Textos | ninguno: los prefijos son datos de Maps, no cadenas de la app |

## Criterios de aceptación

- [x] Con una ruta iniciada en Maps desde una tarjeta de lugar, el log muestra `MapsDestination: place
      card: Maps is going to "…"` y `NavHandoff.noticed` se llama con ese texto. (verificado en la unidad)
- [x] Con el #13 en el build (`main` ya lo tiene), el mapa propio y el fondo de Canvas dibujan la ruta
      a ese destino. (verificado en la unidad)
- [x] Mirar una tarjeta sin pulsar «Iniciar» **no** entrega nada. (test)
- [x] Donde la heurística heredada funciona, sigue siendo ella la que entrega. (`alreadyNoticed`)
- [x] Tests de `MapsCard`: tarjeta en español e inglés, pasos y botones que no son destino, entrega
      solo al arrancar la guía, caducidad a los dos minutos y una entrega por destino. (7 tests, verdes)
- [x] `./gradlew :app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug` (8m38s)
- [x] `./gradlew :app:lintPlayDebug :app:testPlayDebugUnitTest :app:assemblePlayDebug` (9m54s)
- [x] `grep -rn "MINE:" app/src/main` encuentra el punto de contacto (`MapsScreen.kt:75`).

## Riesgos

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Una versión de Maps escribe la tarjeta de otra forma | Medio: no se lee el destino | Los prefijos están en un solo listado, con una prueba por cadena; el log imprime lo leído para enseñar el formato nuevo |
| El conductor abre una tarjeta, la deja abierta y arranca otra ruta después | Bajo | Caducidad de dos minutos y descarte al entregar |
| Maps cambia `NavDirections.running` antes de que se lea la tarjeta | Bajo | El pendiente se entrega en la primera lectura posterior |

## Estado de Aprobación

- [x] Autorización para implementar: el propietario indica continuar con el #15 (2026-10-30)
- [x] Ticket GitHub: [#15](https://github.com/ravalosv/MyDashWheel/issues/15)
- [x] Base: `main` con el #13 y el #14 fusionados
- [x] Implementación: en curso en `feat/#15-maps-card`
