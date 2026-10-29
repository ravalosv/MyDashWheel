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
- Commits: _pendiente de autorización_ (decidir rama: `build/release-signing-local`, que ya tiene
  `8df37b6e`, o una propia).
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
