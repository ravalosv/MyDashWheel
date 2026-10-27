# Puertas de calidad

**G0 — especificación:** issue `ready-for-agent`, dependencias resueltas, criterios verificables. Toda duda de "Dependencias y decisiones pendientes" debe tener tipo (Técnica/Producto), indicar si bloquea desarrollo, y formular pregunta, contexto, sugerencia y respuesta del propietario. Cualquier respuesta `Pendiente` impide aceptar el spec e iniciar construcción. Si no hay dudas, dejar la declaración explícita de que está lista para implementar.

**G0a — pruebas manuales:** antes de marcar un issue Ready, su descripción incluye casos manuales con ID estable `MT-<issue>-NN`, trazabilidad a criterios, precondiciones/datos ficticios, pasos y resultados esperados observables. Escenarios vagos no pasan el gate.

**G1 — diseño:** módulos, contratos entre módulos (`:app`, `:companion`, `:display`, `:link`, `:unitproto`, `:fakeunit`), seguridad y plan revisados.

**G2 — implementación:** pruebas unitarias apropiadas; no cruzar límites de módulos. Verificar compatibilidad con ambas ediciones (github/play).

**G3 — validación:** ejecutar checks reales sobre el flavor por defecto (`github`):
- `./gradlew :app:lintGithubDebug`
- `./gradlew :app:testGithubDebugUnitTest`
- `./gradlew :app:assembleGithubDebug`

Solo verificar la edición Play (`lintPlayDebug`, `testPlayDebugUnitTest`, `assemblePlayDebug`) cuando el cambio toque código compartido entre ambos flavors o afecte específicamente la edición Play.

**G4 — revisión:** revisión de seguridad, permisos Android, compatibilidad de ediciones, privacidad y documentación actualizada.

**G5 — entrega:** reporte de evidencia y aceptación humana. Nunca afirmar PASS por ausencia de tests. Los commits permanecen locales hasta que el propietario verifique manualmente y autorice el push.

## Puerta de regresión final obligatoria (G3/G5)

Antes de entregar desarrollo como terminado o cerrar el issue, el agente ejecuta automáticamente lint, tests unitarios y build de compilación sobre la versión final, verifica que sean satisfactorios y registra evidencia. Las pruebas focalizadas durante la construcción no sustituyen la regresión completa. Fallos, omisiones o bloqueos requeridos impiden PASS/Done/Listo: reportar BLOCKED/incompleto.

## Puerta de compatibilidad

El flavor por defecto es **`github`**. Los cambios deben compilar y funcionar en la edición GitHub como mínimo.

Verificar también la edición **`play`** solo cuando:
- El cambio toque código compartido entre ambos flavors (no específico de `github`)
- El feature afecte explícitamente la edición Play
- Se modifiquen contratos, dependencias o recursos comunes

El código específico de edición se controla mediante `BuildConfig.PLAY` o flavor-specific source sets.

**Verificación GitHub (default, siempre):**
- `./gradlew :app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug`

**Verificación Play (solo cuando aplique):**
- `./gradlew :app:lintPlayDebug :app:testPlayDebugUnitTest :app:assemblePlayDebug`
