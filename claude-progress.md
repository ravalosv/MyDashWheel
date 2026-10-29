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
