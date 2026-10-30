# Instrucciones del repositorio para agentes de codificación

## Misión
Mantener y evolucionar Dashwheel, el launcher de coche abierto para unidades Android (QF, FYT, Microntek y más), con mapas, música y datos del vehículo en una sola pantalla. Kotlin + Jetpack Compose + Gradle multi-módulo.

## Antes de trabajar
1. Lee `README.md`.
2. Lee `docs/agents/domain.md` y el `GLOSSARY.md` raíz si existen.
3. Revisa los ADR relevantes en `docs/adr/`.
4. Identifica el issue de GitHub activo (`gh issue list --state open --label "ready-for-agent"`).
5. Comprueba dependencias y preguntas abiertas en el issue.
6. Si una decisión P0 está sin resolver y afecta al cambio, pregunta antes de implementar.
7. Usa el número de issue como identificador canónico en branches y commits (ej. `feat/#42-widget-settings`).
8. Revisa `docs/harness/quality-gates.md` antes de proponer un plan.
9. Al crear o modificar pantallas/widgets, revisa los screenshots en `docs/screenshots/` como referencia visual para estilo y composición.
10. Ejecuta `./init.sh` (o `init.ps1` en Windows) para verificar el estado base.

## Reglas obligatorias
- No introduzcas microservicios, frameworks HTTP ni dependencias estructurales sin ADR aprobado.
- El dominio (lógica de negocio) no debe depender de clases de Android UI.
- Los módulos (`:app`, `:companion`, `:display`, `:link`, `:unitproto`, `:fakeunit`) deben respetar sus límites; no accedas a implementaciones privadas de otro módulo.
- Los contratos entre módulos usan interfaces o protobufs (`:unitproto`, `:link`).
- No confíes en el cliente para validación de seguridad; las operaciones sensibles se validan en el backend cuando aplique.
- No registres secretos, tokens de API, claves de Gemini ni PII en código o logs.
- No afirmes que un test, build o lint se ejecutó si no se ejecutó realmente.
- No hagas push de commits ni ramas sin autorización explícita del propietario.
- Todo commit y toda rama nueva parten de `main` (`CLAUDE.md`, "Regla de ramas y commits"). Comprueba la rama actual antes de commitear; `releases` solo recibe merges de `main`.
- Mantén la compatibilidad con Android 9+ (minSdk 28) salvo ADR que indique lo contrario.
- Las dos ediciones (github/play) se construyen desde la misma base; no rompas la edición Play al añadir features de root.
- Los cambios de UI deben funcionar en pantallas horizontales, verticales y de cualquier resolución.

## Flujo por issue
Antes de presentar un desarrollo como terminado, el agente debe ejecutar y verificar los checks de calidad configurados sobre la versión final. Es obligatorio cumplir el gate de `docs/harness/quality-gates.md`: pruebas fallidas, omitidas, ausentes o bloqueadas impiden Done/Listo y se reportan como incompleto/BLOCKED.

1. Resumir objetivo y criterios de aceptación.
2. Revisar especificaciones, dependencias y ADR.
3. Proponer impacto en módulos, API, datos, seguridad y pruebas.
4. Implementar el cambio mínimo completo.
5. Ejecutar lint, tests unitarios y build de compilación. El flavor por defecto es `github`:
   - `./gradlew :app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug`
   Verificar también `play` solo si el cambio toca código compartido o afecta esa edición.
6. Revisar compatibilidad entre ediciones (github/play), configuraciones de pantalla y permisos.
7. Actualizar documentación/ADR cuando corresponda.
8. Emitir reporte usando `docs/harness/templates/implementation-report.md`.
