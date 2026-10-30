# Plan: motor de localización propio para la navegación en Android < 11

**Fecha:** 2026-10-30
**Estado:** Autorizada (el propietario autoriza este enfoque el 2026-10-30)

**GitHub Ticket:** [#12](https://github.com/ravalosv/MyDashWheel/issues/12) en `ravalosv/MyDashWheel`

---

## Contexto

Crash al iniciar una ruta desde el widget en un unit `alps FF_866X` (mt6768), firmware Android 10
(`QP1A.190711.020`, `SDK_INT` 29) aunque se anuncie como 12:

```
java.lang.AbstractMethodError: abstract method
  "void android.location.LocationListener.onProviderDisabled(java.lang.String)"
  at android.location.LocationManager$ListenerTransport._handleMessage
```

Comprobado por el propietario: **sin iniciar ruta no crashea**.

## Causa

`LocationListener` tiene implementaciones `default` en el SDK desde API 30. Una clase compilada
contra `compileSdk` ≥ 30 que no sobrescribe `onProviderDisabled` funciona en Android 11+, pero en
API < 30 el método sigue siendo abstracto: el framework lo invoca vía `ListenerTransport` y lanza
`AbstractMethodError`.

Los tres `LocationListener` del repo (`LiveFeeds`, `PhoneGpsShare`, `RoadWatch`) sí lo sobrescriben,
así que el listener que falla es de una librería. El único que registra en ese momento es el motor de
navegación que `InAppNav.start` construye:

```kotlin
AndroidMapLibreNavigation(app, MapLibreNavigationOptions(), MapLibreLocationEngine(app, Looper.getMainLooper()))
```

## Decisión

Para **API < 30** —donde el fallo es posible—, la navegación usa un `LocationEngine` nuestro
alimentado por `LocationFeed.location`, el mismo `StateFlow` que ya mueve el mapa y el puck. De
API 30 en adelante no se toca nada: se sigue usando `MapLibreLocationEngine`.

La condición es `SDK_INT`, no el modelo del unit:

- Cubre cualquier aparato con el mismo fallo, sea del fabricante que sea.
- No se rompe si ese firmware se actualiza.
- No desactiva el motor de la librería en aparatos donde funciona.

## Aislamiento (regla de desacoplamiento del fork)

| Pieza | Dónde |
|---|---|
| `FeedLocationEngine` (implementación) | `app/src/main/java/com/openauto/dash/mine/FeedLocationEngine.kt` — archivo nuevo, paquete `com.openauto.dash.mine` |
| `MineOverrides.navLocationEngine(context)` (selector por `SDK_INT`) | `app/src/main/java/com/openauto/dash/mine/MineOverrides.kt` — archivo nuevo |
| Enganche | `InAppNav.kt`: **una línea**, marcada `// MINE: #12` |

Todo lo demás del fork original queda intacto, y tras un merge del upstream `grep -rn "MINE:"` señala
el punto de contacto.

## Cambios

1. `FeedLocationEngine : LocationEngine` (maplibre-navigation-core 5.0.0-pre8):
   - `listenToLocation(request)` → `callbackFlow` sobre `LocationFeed.location`, con
     `LocationFeed.acquire(context)` al abrir y `release()` al cerrar.
   - `getLastLocation()` → el último fix conocido, o `null`.
   - Convierte `android.location.Location` al `Location` de la librería (lat/lon, altitud, rumbo,
     velocidad, precisión, tiempo).
2. `MineOverrides.navLocationEngine(context)`: `SDK_INT < R` → `FeedLocationEngine`, si no
   `MapLibreLocationEngine(context, Looper.getMainLooper())`.
3. `InAppNav.start`: usar `MineOverrides.navLocationEngine(app)`.

## Consecuencias

- En API < 30 desaparece la segunda suscripción GPS mientras se navega: mapa, puck y navegación leen
  el mismo feed.
- En API ≥ 30 el comportamiento es exactamente el de hoy.
- El `Flow` del motor se cierra al parar la navegación (`stop()` → `nav.onDestroy()`), así que el
  `acquire/release` del feed queda equilibrado.

## Criterios de aceptación

- [x] Compila en ambas ediciones (`:app:compileGithubDebugKotlin` en 2m 1s).
- [x] `:app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug` → BUILD SUCCESSFUL.
- [x] `:app:lintPlayDebug :app:testPlayDebugUnitTest :app:assemblePlayDebug` → BUILD SUCCESSFUL.
- [x] Test unitario del selector: con `SDK_INT` bajo devuelve nuestro motor; con API ≥ 30, el de la
      librería (`MineOverridesTest`, en la verificación de ambas ediciones).
- [x] En el unit (`FF_866X`, API 29): buscar dirección en el widget → iniciar ruta → guía sin crash.
      Verificado por el propietario el 2026-10-30 con la versión publicada `v1.0.882`.
- [ ] Sin regresión en el unit moderno (API ≥ 30): la navegación sigue guiando como antes. Pendiente
      en dispositivo; en código, esa rama ejecuta exactamente la construcción anterior.

**Commit:** `fe4c725a` en `main` y en `releases` (2026-10-30). Ticket
[#12](https://github.com/ravalosv/MyDashWheel/issues/12) cerrado el mismo día, con la prueba en el
aparato anotada como pendiente: el crash solo se reproduce al iniciar una ruta en el unit afectado.

**Prueba en el aparato:** verificada por el propietario el 2026-10-30 con la versión publicada
`v1.0.882` (APK de CI, no el build local): iniciar una ruta desde el widget ya no crashea.

## Riesgos

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Nuestro motor no reproduce exactamente el contrato de la librería (frecuencia, precisión) | Medio: guía con fixes distintos | Se alimenta del mismo GPS que el mapa, a 1 s; la librería solo consume posiciones |
| Desbalance en `acquire/release` de `LocationFeed` | Medio: GPS encendido de más | Cierre del `Flow` en `stop()`; test del ciclo |
| Un merge del upstream reescribe `InAppNav.start` y pierde la llamada | Bajo | Marca `// MINE: #12` y `grep` obligatorio tras cada merge |
| No poder probar en API ≥ 30 en local | Bajo | La rama `else` es el código actual, sin cambios |

## Estado de Aprobación

- [x] Autorización para implementar: "si, implementalo" con el enfoque de gate por `SDK_INT`
      (2026-10-30)
- [x] Ticket GitHub: [#12](https://github.com/ravalosv/MyDashWheel/issues/12)
