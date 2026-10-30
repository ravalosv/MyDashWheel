# Registro de Progreso

## Estado Verificado Actual

- Raíz del repositorio: E:\Mis Proyectos\Home\Dashwheel
- Ruta de inicio estándar: `./init.sh` o `./init.ps1`
- Ruta de verificación estándar: `./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug`
- Característica inacabada de mayor prioridad actual: _pendiente_
- Bloqueador actual: _ninguno_

## Registro de Sesiones

### Sesión 001

- Fecha: 2026-10-27
- Objetivo: Agregar harness de Anthropic adaptado a Dashwheel
- Completado: Creada estructura completa de harness en docs/harness/, AGENTS.md, init scripts, y configuración de Claude Code
- Verificación ejecutada: N/A (setup inicial)
- Evidencia capturada: Archivos creados
- Commits: _pendiente_
- Archivos o artefactos actualizados:
  - AGENTS.md
  - init.sh, init.ps1
  - claude-progress.md
  - feature_list.json
  - docs/harness/quality-gates.md
  - docs/harness/workflows/implement-story.md
  - docs/harness/workflows/prepare-story.md
  - docs/harness/checklists/definition-of-done.md
  - docs/harness/checklists/security-review.md
  - docs/harness/templates/implementation-report.md
  - docs/harness/templates/story-plan.md
  - .claude/settings.example.json
  - .claude/agents/architecture-reviewer.md
  - .claude/agents/security-reviewer.md
  - .claude/agents/test-reviewer.md
  - .claude/commands/adr-proposal.md
  - .claude/commands/implement-story.md
  - .claude/commands/prepare-story.md
  - .claude/commands/review-story.md
- Riesgo conocido o problema sin resolver: _ninguno_
- Mejor próximo paso: _pendiente de definición por el propietario_

### Sesión 002

- Fecha: 2026-10-29
- Objetivo: Cerrar los tickets #1 (lugares personalizados) y #3 (firma de release con claves propias)
- Completado:
  - Ticket #1: lugares personalizados en "Mis lugares", widget 1-4 lugares, renombrar Casa/Trabajo,
    búsqueda por dirección y por nombre de negocio/POI, listado de hasta 5 coincidencias (también en
    Casa/Trabajo) y cierre del listado con ✕ o al editar el campo.
  - Ticket #3: firma de release con keystore propio en local. `gradle/release-signing.gradle.kts`
    resuelve env vars (CI) → `keys/keystore.properties` (local) → clave debug con aviso.
    `keys/release.keystore` (RSA 4096, alias `dashwheel`, válido hasta 2054).
  - `.vscode/launch.json` y `tasks.json`: configuraciones/tareas de release github y play.
- Verificación ejecutada:
  - `:app:lintGithubDebug` BUILD SUCCESSFUL (0 errores), `:app:testGithubDebugUnitTest` sin fallos,
    `:app:assembleGithubDebug` BUILD SUCCESSFUL.
  - `:app:assembleGithubRelease :companion:assembleGithubRelease` BUILD SUCCESSFUL;
    `apksigner verify` confirma `CN=Dashwheel, O=Dashwheel Personal, C=MX`.
  - `:app:signingReport`: `githubRelease` y `playRelease` → `keys/release.keystore`.
  - Pruebas manuales MT-places-01 a MT-places-07 en dispositivo: OK (propietario).
- Evidencia capturada: APKs `app-github-release.apk` (48 MB) y `companion-github-release.apk` (36 MB);
  SHA-256 del certificado `7dbc6c4a5c24d7374307bdcf4d4cb481da6232f7789a60a5802df5d4ddfcbe44`.
- Commits: `9df8b53f` en `feat/places-personalizados-desde-ticket-1` (ticket #1). Firma de release
  (ticket #3): _pendiente de autorización de commit_.
- Archivos o artefactos actualizados:
  - app/src/main/java/com/openauto/dash/{Places,HandsFreeSettings,HomeWorkTile,MapLibrePanel}.kt
  - app/src/main/res/values*/strings_places.xml (10 idiomas)
  - gradle/release-signing.gradle.kts, app/build.gradle.kts, companion/build.gradle.kts
  - .vscode/launch.json, .vscode/tasks.json
  - docs/harness/reports/places-personalizados-plan.md, docs/harness/reports/release-signing-local-plan.md
- Riesgo conocido o problema sin resolver:
  - **La versión no se incrementa en local.** `versionCode`/`versionName` caen a `1`/`1.0` si no se
    exportan `VERSION_CODE`/`VERSION_NAME`; solo CI los calcula (commits de `main` + 200). Un release
    local instalado encima de otro falla con `INSTALL_FAILED_VERSION_DOWNGRADE`. Pendiente decidir si
    el build local calcula el código solo (propuesta: misma fórmula que CI, con la env var como override).
  - La clave de `keys/` es nueva: no actualiza in-place instalaciones firmadas por las releases de
    upstream. Respaldar `keys/release.keystore` + `keys/keystore.properties` fuera del equipo.
  - El geocodificador no corrige erratas ("plannet fitness" ≠ "planet fitness").
- Mejor próximo paso: decidir e implementar (con ticket) el cálculo automático del `versionCode` local.

### Sesión 003

- Fecha: 2026-10-29
- Objetivo: Ticket #6 — rebrand a MyDashwheel (nombre de release, `applicationId`, publicación propia) y
  validación de los secretos de firma en CI
- Completado:
  - `applicationId` → `com.mydashwheel.dash` (play `…dash.play`); companion
    `com.mydashwheel.dash.companion` (play `…companion.play`).
  - Updater in-app → `ravalosv/MyDashWheel` (`BuildConfig.GITHUB_OWNER`/`GITHUB_REPO`); assets
    `MyDashwheel-<v>.apk` y `mydashwheel-companion.apk`; títulos de release y de promoción.
  - Cadenas que son identidad de la app pasan a `BuildConfig.APPLICATION_ID` (`PrivApp`,
    `SystemDisplayHelper`, `WindowListing`); los prefijos de `Automation` se conservan a propósito.
  - Etiqueta visible (decidida por el propietario ese mismo día): `MyDashwheel` y
    `MyDashwheel Companion` en los 11 idiomas.
  - `PipAnchorTest`: los fixtures de `dumpsys` derivan el paquete del build en lugar de escribirlo.
- Verificación ejecutada:
  - `:app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug
    :companion:assembleGithubDebug` → BUILD SUCCESSFUL (lint 0 errores).
  - `:app:assembleGithubRelease :companion:assembleGithubRelease` → BUILD SUCCESSFUL;
    `aapt2 dump badging` da `com.mydashwheel.dash` / `com.mydashwheel.dash.companion` y
    `application-label:'MyDashwheel'` / `'MyDashwheel Companion'`.
  - Secretos de firma: los tres niveles de validación documentados en
    `docs/harness/reports/release-signing-local-plan.md`. Nivel 2 local OK; Nivel 3 en CI
    (run `40955976901`, `success`) OK, ambos APK con la huella `7dbc6c4a…be44`.
- Commits: `ddc4035f` (rebrand) y `16c3f36d` (restauración de `gradlew.bat`) en
  `feat/rebrand-mydashwheel`; fusionados en `main` por PR #7 (`94be5f43`) y PR #8 (`aeeb44cd`).
  Ticket #6 cerrado el 2026-10-29.
- Incidencia corregida: un commit intermedio metió `*.bat` en `.gitignore` y borró `gradlew.bat`
  (CI en Linux no lo detecta porque usa `./gradlew`). Se restauró el archivo y se añadió
  `!gradlew.bat`. Regla para el futuro: `*.bat` nunca debe ignorar el wrapper de Gradle.
- Archivos o artefactos actualizados:
  - app/build.gradle.kts, companion/build.gradle.kts, gradle/release-signing.gradle.kts
  - app/src/github/java/com/openauto/dash/PrivApp.kt, app/src/main/java/com/openauto/dash/{SystemDisplayHelper,WindowListing,UpdateManager}.kt
  - app/src/main/res/values/strings.xml, companion/src/main/res/values*/strings.xml
  - app/src/test/java/com/openauto/dash/{PipAnchorTest,UpdateManagerTest}.kt
  - .github/workflows/build.yml, .github/workflows/promote.yml, .vscode/{launch,tasks}.json
  - docs/harness/reports/rebrand-mydashwheel-plan.md, docs/harness/reports/release-signing-local-plan.md
- Riesgo conocido o problema sin resolver:
  - Nada verificado aún en dispositivo: `adb install -r` + nombre del lanzador.
  - El release de CI anterior al rebrand publica `Dashwheel-<v>.apk` con `com.openauto.dash`; se
    instala **al lado** de la versión rebrandeada (desinstalar la anterior).
  - Sigue abierto el `versionCode` local (Sesión 002).
- Mejor próximo paso: autorizar y ejecutar el commit del rebrand, cerrar el ticket #6 y probar en el
  dispositivo.

### Sesión 004

- Fecha: 2026-10-29
- Objetivo: traer `upstream/main` a `main` y hacer que publicar releases sea un acto deliberado
  (ticket #9)
- Completado:
  - Merge de `upstream/main` (19 commits, 133 archivos: HCN/Junsun, modo 2 del volante, foco de
    pantalla, segunda pantalla en la Pi, refactor de la cámara del mapa). Conflictos en
    `MapLibrePanel` (se tomó el refactor de upstream) y `WindowListing` (se combinó su
    `pickFloatingWindow` con `BuildConfig.APPLICATION_ID`).
  - `build.yml`: el job `release` solo corre en `workflow_dispatch` o al pushear `releases`; las
    notas y la pre-release beta pasan a `refs/heads/releases`; se retira el pre-release de rama.
    Los checks siguen en cada push y PR.
  - `releases` creada desde `main` y avanzada por fast-forward.
- Verificación ejecutada:
  - Gate completo tras el merge: `lintGithubDebug` + `testGithubDebugUnitTest` +
    `assembleGithubDebug` (app y companion) → BUILD SUCCESSFUL (7m 45s).
  - YAML de `build.yml` y `promote.yml` válido (`js-yaml`).
- Commits: `7c251c60` (merge de upstream) y `2d5b3cfe` (gate de publicación), en `main` y en `releases`.
- Riesgo conocido o problema sin resolver:
  - **Los runs no se esperaron** (decisión del propietario): falta confirmar que el push a `main` deja
    `release` en `skipped` y que el de `releases` publica la beta. Si no, reabrir #9 o abrir otro ticket.
  - `main` no tiene protección de rama: acepta pushes directos.
  - Los pre-releases de rama ya no existen; para probar una rama en el head unit queda
    `workflow_dispatch` o fusionarla en `releases`.
- Mejor próximo paso: revisar los runs `40981094726` (main) y `40985274625` (releases) cuando terminen.

### Sesión 005

- Fecha: 2026-10-29
- Objetivo: reactivar la autoactualización apuntando a los releases del fork (ticket #10)
- Completado:
  - `AutomotiveDashboard.kt`: reactivados el bucle de chequeo (`Edition.full`: al arrancar, en cada
    encendido y cada 6 h), la auto-descarga (permiso de instalación + red sin medir, una vez por
    build), el aviso "Update ready" (solo aparcado, una vez por build) y `UPDATE_CHECK_EVERY_MS`.
  - Sin imports nuevos: `delay`, `launch`, `mutableLongStateOf` e `ignitionFlow` ya estaban.
- Verificación ejecutada:
  - `:app:lintGithubDebug` sin errores, `:app:testGithubDebugUnitTest` sin fallos,
    `:app:assembleGithubDebug` → BUILD SUCCESSFUL (6m 40s).
- Commits: `dbcad265` en `main`. Ticket #10 cerrado.
- Riesgo conocido o problema sin resolver:
  - **Sin probar en dispositivo**: falta un release publicado por `releases` encima de una versión
    instalada. `releases` sigue en `2d5b3cfe`, así que el cambio aún no está publicado.
  - El bucle consulta cada 6 h y en cada encendido: en la tablet eso implica red móvil si no hay wifi
    (la descarga solo ocurre en red no medida).
- Mejor próximo paso: merge de `main` a `releases` para publicar la beta con la autoactualización y
  probar el ciclo completo en el head unit.

---

## Sesión 006 (2026-10-29): la CI en rojo por un fixture de la edición Play

- Objetivo: explicar y corregir el fallo de `:app:testPlayDebugUnitTest` que llevaba tres pushes
  consecutivos en rojo (`40986702523`, `40988165283`, `40988546348`).
- Causa: el rebrand del ticket #6 cambió los fixtures de `PipAnchorTest` a `BuildConfig.APPLICATION_ID`,
  pero la aserción del resumen siguió comparando con el literal `"fullscreen dash · freeform maps"`.
  `summarizeStacks` nombra la app con el último segmento del paquete, así que en `github`
  (`com.mydashwheel.dash`) la etiqueta es `dash` y coincidía; en `play`
  (`com.mydashwheel.dash.play`) es `play` y fallaba. El fallo era del fixture, no de `WindowListing`.
- Cambio: `selfName = self.substringAfterLast('.')` y la aserción usa `"fullscreen $selfName · freeform maps"`.
  Sin tocar código de producción.
- Verificación ejecutada:
  - `:app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug` → BUILD SUCCESSFUL (7m 6s).
  - `:app:lintPlayDebug :app:testPlayDebugUnitTest :app:assemblePlayDebug` → BUILD SUCCESSFUL (13m 9s).
- Plan: `docs/harness/reports/play-test-label-plan.md`. Ticket #11.
- Cierre: commit `85bb4aca` publicado en `main`; CI en verde en el run `41004891688`. Ticket #11
  cerrado el 2026-10-30.
- Incidente: el commit se creó por error sobre la rama `releases` (era la rama activa). Se recuperó
  con `cherry-pick` sobre `main`; ningún commit se perdió. Al corregir las refs, un `reset --hard`
  cruzado invirtió `main` y `releases` en local; se corrigió y ambas coincidieron con el remoto.
  Consecuencia: nueva "Regla de ramas y commits" en `CLAUDE.md` (todo commit y toda rama parten de
  `main` salvo indicación expresa).
- Riesgo conocido o problema sin resolver:
  - El push a `releases` (run `41005993412`, en curso al escribir esto) publica el beta con la
    autoactualización (#10) y esta corrección; la prueba en dispositivo sigue pendiente.
  - Los builds locales siguen con `versionCode 1`: un APK local no se instala encima de uno de CI.
- Mejor próximo paso: confirmar el run `41005993412` y probar la instalación del beta publicado.

---

## Sesión 007 (2026-10-30): crash de navegación en Android < 11

- Objetivo: resolver el ticket #12 — al iniciar una ruta desde el widget, el proceso moría con
  `AbstractMethodError` en `LocationListener.onProviderDisabled` en un unit con `SDK_INT` 29.
- Causa: el motor de maplibre-navigation registra un listener compilado contra API 30+, donde esos
  callbacks son `default` y por eso no los sobrescribe; en API < 30 siguen abstractos y el
  `ListenerTransport` del sistema los invoca igual. Los tres listeners propios del repo sí los
  implementan, así que el que fallaba era el de la librería, creado al arrancar la guía.
- Cambio: bajo API 30, `InAppNav` pide el motor a `MineOverrides.navLocationEngine`, que devuelve un
  `FeedLocationEngine` sobre `LocationFeed`; de API 30 en adelante sigue `MapLibreLocationEngine`.
  Una sola línea marcada `// MINE: #12` en el archivo heredado.
- Aislamiento: nueva "Regla de desacoplamiento del fork" en `CLAUDE.md` (paquete
  `com.openauto.dash.mine`, marcas `// MINE:`, `grep` obligatorio tras cada merge del upstream) y
  puntero en `AGENTS.md`.
- Verificación ejecutada: `compileGithubDebugKotlin` (2m 1s); `lintGithubDebug`,
  `testGithubDebugUnitTest`, `assembleGithubDebug` y el equivalente Play → BUILD SUCCESSFUL.
- Commit: `fe4c725a` en `main`, fusionado en `releases`. Ticket #12 cerrado.
- Riesgo conocido o problema sin resolver:
  - La prueba en el aparato sigue pendiente: instalar el APK de `fe4c725a` en el `FF_866X` y repetir
    el escenario (buscar dirección → iniciar ruta).
  - Si el crash reapareciera, `adb logcat` debe mostrar si viene de otra ruta de código.
- Mejor próximo paso: probar en el unit y, si guía bien, publicar el beta desde `releases`.
