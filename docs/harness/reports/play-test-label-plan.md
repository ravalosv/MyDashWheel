# Plan: arreglar `PipAnchorTest` en la edición Play

**Fecha:** 2026-10-29
**Estado:** Autorizada (el propietario reporta el fallo de CI y pide corregirlo, 2026-10-29)

**GitHub Ticket:** [#11](https://github.com/ravalosv/MyDashWheel/issues/11) en `ravalosv/MyDashWheel`

---

## Contexto

Tres pushes consecutivos a `main` acabaron en rojo por el mismo test:

```
PipAnchorTest > freeformWindowIsFoundWhenThereIsNoPip FAILED
1331 tests completed, 1 failed
```

Runs: `40986702523`, `40988165283`, `40988546348`. El resto de tareas pasan, incluido
`testGithubDebugUnitTest` y los dos `lint`.

## Causa

El ticket #6 (rebrand) sustituyó en los fixtures de `PipAnchorTest` el literal `com.openauto.dash`
por `BuildConfig.APPLICATION_ID` (`self`). La aserción de la etiqueta del resumen se quedó con el
texto escrito a mano:

```kotlin
assertEquals("fullscreen dash · freeform maps", WindowListing.summarizeStacks(freeform))
```

`WindowListing.summarizeStacks` nombra cada app con el último segmento del paquete
(`pkg.substringAfterLast('.')`), y desde el rebrand ese segmento depende del flavor:

| Flavor | `applicationId` | Etiqueta | Resultado |
|---|---|---|---|
| github | `com.mydashwheel.dash` | `dash` | coincide por casualidad |
| play | `com.mydashwheel.dash.play` | `play` | falla |

## Decisión

Alinear el fixture con los demás: derivar la etiqueta esperada de `self` en el propio test.

```kotlin
/** Cómo [WindowListing.summarizeStacks] nombra la app: el último segmento del paquete. */
private val selfName = self.substringAfterLast('.')

assertEquals("fullscreen $selfName · freeform maps", WindowListing.summarizeStacks(freeform))
```

No se toca código de producción: `WindowListing` se comporta como debe. Cambiar `summarizeStacks`
para que ignorara el sufijo de flavor sería empeorar el código para complacer a un fixture.

## Consecuencias

- El test deja de depender del flavor: sirve tal cual para `github` y `play`, y para cualquier
  `applicationId` futuro.
- Cualquier otra aserción que codificara el nombre del paquete se revisó: esta era la única
  (`grep` de `"dash` en `app/src/test` y `companion/src/test`).
- Nada cambia en el APK publicado: es una corrección de test, no de comportamiento.

## Criterios de aceptación

- [x] `:app:testPlayDebugUnitTest` en verde (el run que hoy falla) — 13m 9s, `BUILD SUCCESSFUL`.
- [x] `:app:testGithubDebugUnitTest` sigue en verde — 7m 6s, `BUILD SUCCESSFUL`.
- [x] `./gradlew :app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug` OK.
- [x] `./gradlew :app:lintPlayDebug :app:testPlayDebugUnitTest :app:assemblePlayDebug` OK.
- [x] CI en verde en el push de la corrección — run `41004891688` sobre `85bb4aca`: `success`
      (lint github/play, tests github/play, assemble de ambas ediciones).

**Commit:** `85bb4aca` en `main` (2026-10-29). Ticket
[#11](https://github.com/ravalosv/MyDashWheel/issues/11) cerrado el 2026-10-30.

**Incidente durante el cierre:** el commit se creó por error sobre la rama `releases` (era la rama
activa de la sesión). Se recuperó con `cherry-pick` sobre `main` y publicando `85bb4aca`; ningún
commit se perdió y `main` quedó como única fuente. De ahí la regla "Regla de ramas y commits" añadida
a `CLAUDE.md`.

## Riesgos

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Otra aserción depende del nombre del paquete y no se vio | Bajo | `grep` de la etiqueta en los dos módulos de test; solo aparecía esta línea |
| El fallo real fuese de `WindowListing` y no del fixture | Bajo | La traza muestra la aserción de la etiqueta, y el flavor `github` pasa con el mismo código |

## Estado de Aprobación

- [x] Autorización para implementar: el propietario reporta el fallo pidiendo la corrección (2026-10-29)
- [x] Ticket GitHub creado: [#11](https://github.com/ravalosv/MyDashWheel/issues/11)
