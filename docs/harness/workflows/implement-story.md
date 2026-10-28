# Workflow: implementar issue

1. Antes de cualquier edición, revisar la sección de dependencias y decisiones pendientes del issue.
2. Si alguna decisión bloqueante está sin resolver, detenerse y preguntar al propietario.
3. Confirmar issue con label `ready-for-agent`.
4. Leer contexto y registrar plan/impacto antes de editar.
5. Escribir pruebas de reglas críticas y casos negativos; implementar caso vertical mínimo.
6. Validar compatibilidad entre ediciones (github/play), configuraciones de pantalla y permisos Android.
7. Antes de entregar como terminado, ejecutar automáticamente el flavor por defecto (`github`):
   - `./gradlew :app:lintGithubDebug` (y módulos afectados)
   - `./gradlew :app:testGithubDebugUnitTest`
   - `./gradlew :app:assembleGithubDebug`
   Verificar también la edición `play` solo si el cambio toca código compartido o afecta esa edición:
   - `./gradlew :app:lintPlayDebug`
   - `./gradlew :app:testPlayDebugUnitTest`
   - `./gradlew :app:assemblePlayDebug`
   Registrar comandos, fecha, resultados y códigos de salida.
8. Una comprobación fallida, omitida o bloqueada impide Done/Listo; corregir o reportar BLOCKED/incompleto.
9. Actualizar ADR y documentación si cambian contratos entre módulos.
10. Emitir reporte usando `docs/harness/templates/implementation-report.md`.
11. Solicitar revisión y aceptación; registrar resultado manual Pass/Fail/Blocked con fecha, ejecutor y evidencia solo tras ejecutar cada caso.
12. Mantener los commits localmente para la verificación manual del propietario. No hacer push sin autorización explícita.
