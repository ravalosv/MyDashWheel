# Instrucciones del repositorio para agentes de codificación

## Misión
Mantener y evolucionar Dashwheel, el launcher de coche abierto para unidades Android (QF, FYT, Microntek y más), con mapas, música y datos del vehículo en una sola pantalla. Kotlin + Jetpack Compose + Gradle multi-módulo.

## Antes de trabajar
1. Lee `README.md`.
2. Lee `docs/agents/domain.md` y el `GLOSSARY.md` raíz si existen.
3. Revisa los ADR relevantes en `docs/adr/`.
4. Identifica el issue solicitado en `ravalosv/MyDashWheel`; no se requieren etiquetas del upstream. Consulta `docs/agents/issue-tracker.md`.
5. Comprueba dependencias y preguntas abiertas en el issue.
6. Si una decisión P0 está sin resolver y afecta al cambio, pregunta antes de implementar.
7. Usa el número de issue como identificador canónico en branches y commits (ej. `feat/#42-widget-settings`).
8. Revisa `docs/harness/quality-gates.md` antes de proponer un plan.
9. Al crear o modificar pantallas/widgets, revisa los screenshots en `docs/screenshots/` como referencia visual para estilo y composición.
10. Consulta el estado local con `./init.sh` (o `.\init.ps1` en Windows); no ejecuta Gradle por defecto.

## Reglas obligatorias
- Para Git/GitHub de este repositorio, usar exclusivamente la cuenta autenticada `ravalosv`. Antes de operaciones remotas, comprobar `gh api user --jq .login`; si devuelve otra cuenta, activar `ravalosv` con `gh auth switch --hostname github.com --user ravalosv` y volver a verificar. Si no está disponible, detenerse; nunca usar otra cuenta ni cambiar configuración global. Ver detalles en `docs/agents/issue-tracker.md`.
- No introduzcas microservicios, frameworks HTTP ni dependencias estructurales sin ADR aprobado.
- El dominio (lógica de negocio) no debe depender de clases de Android UI.
- Los módulos (`:app`, `:companion`, `:display`, `:link`, `:unitproto`, `:fakeunit`) deben respetar sus límites; no accedas a implementaciones privadas de otro módulo.
- Los contratos entre módulos usan interfaces o protobufs (`:unitproto`, `:link`).
- No confíes en el cliente para validación de seguridad; las operaciones sensibles se validan en el backend cuando aplique.
- No registres secretos, tokens de API, claves de Gemini ni PII en código o logs.
- No afirmes que un test, build o lint se ejecutó si no se ejecutó realmente.
- No hagas push de commits ni ramas sin autorización explícita del propietario.
- Todo commit y toda rama nueva parten de `main` (`CLAUDE.md`, "Regla de ramas y commits"). Comprueba la rama actual antes de commitear; `releases` solo recibe merges de `main`.
- Es un fork que se mergea con el upstream: el código propio va en `com.openauto.dash.mine` y en los archivos heredados solo queda una llamada marcada `// MINE:` (`CLAUDE.md`, "Regla de desacoplamiento del fork").
- Mantén la compatibilidad con Android 9+ (minSdk 28) salvo ADR que indique lo contrario.
- Las dos ediciones (github/play) se construyen desde la misma base; no rompas la edición Play al añadir features de root.
- Los cambios de UI deben funcionar en pantallas horizontales, verticales y de cualquier resolución.

## Flujo por issue
Los issues de este fork viven en `ravalosv/MyDashWheel`. No apliques estados, etiquetas de triage ni gates de `deviloufr-ai/Dashwheel`; `ready-for-agent` no es requisito. Confirma autorización explícita y decisiones bloqueantes antes de implementar.

La política canónica es [quality-gates.md](docs/harness/quality-gates.md): validación focalizada por riesgo durante desarrollo y regresión GitHub una vez por lote aceptado, antes de APK para uso habitual o cierre. Distinguir listo para revisión de validado para uso habitual. Fallos u omisiones de checks requeridos bloquean la etapa correspondiente; checks fuera de alcance se declaran No requeridos.

1. Resumir objetivo y criterios de aceptación.
2. Revisar especificaciones, dependencias y ADR.
3. Resumir impacto y pruebas; planes extensos y revisores especializados solo si el riesgo lo justifica.
4. Implementar el cambio mínimo completo.
5. Ejecutar los checks del alcance definido en el gate, reutilizando evidencia válida de la misma versión y entorno. Documentación no requiere Gradle; scripts del harness solo requieren validación propia.
   Play se verifica solo por solicitud explícita; conservar su código y declarar si no se verificó.
6. Revisar compatibilidad entre ediciones (github/play), configuraciones de pantalla y permisos.
7. Actualizar documentación/ADR cuando corresponda.
8. Emitir reporte breve usando `docs/harness/templates/implementation-report.md`, sin archivos adicionales por rutina.
