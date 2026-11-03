# Puertas de calidad

Esta es la política canónica de validación del fork personal. Sustituye las reglas anteriores de regresión por cada iteración; los reportes históricos conservan su evidencia original.

**G0 — especificación:** issue identificado en `ravalosv/MyDashWheel`, autorización explícita, criterios verificables y decisiones bloqueantes resueltas. No se requieren etiquetas del upstream. Una duda no bloqueante puede quedar pendiente si se documenta su impacto.

**G0a — pruebas manuales:** definir solo los casos necesarios para el comportamiento afectado que no cubran las pruebas automatizadas. Para UI, hardware y permisos, usar ID `MT-<issue>-NN`, criterio, precondiciones, pasos y resultado observable. No exigir casos manuales para documentación ni duplicar tests sin motivo.

**G1 — diseño:** resumir alcance, módulos, riesgos y pruebas en el issue o conversación. Plan extenso, ADR y revisión especializada solo cuando el riesgo o un cambio de arquitectura lo justifiquen; no crear archivos de planificación por rutina.

**G2 — implementación:** mantener límites modulares, Android 9+ y el código de ambas ediciones. Probar reglas nuevas, regresiones corregidas y casos negativos relevantes.

## G3 — validación por riesgo durante desarrollo

Elegir el alcance antes de ejecutar comandos y ampliarlo si aparecen fallos o efectos en otros módulos.

| Cambio | Validación requerida |
|--------|----------------------|
| Documentación/instrucciones del harness | Coherencia y diff; sin Gradle |
| Scripts o workflows del harness | Sintaxis y comportamiento afectado; sin build de la app |
| Código localizado | Compilación GitHub y tests focalizados del comportamiento afectado |
| UI/widgets/recursos | Lo anterior, cuando corresponda, y comprobación visual del flujo y orientaciones afectadas |
| Persistencia/migraciones/contratos | Compilación y tests de módulos y consumidores afectados, incluidos errores y compatibilidad de datos |
| Permisos/root/dependencias/merge upstream | Lint, tests y build de los módulos afectados; ampliar a regresión GitHub si el impacto es transversal |

Ejemplo para código de `:app` (sustituir el selector por los tests reales afectados):

```powershell
.\gradlew.bat :app:compileGithubDebugKotlin :app:testGithubDebugUnitTest --tests "com.openauto.dash.mine.ClaseAfectadaTest"
```

No usar un selector inexistente ni afirmar PASS por ausencia de tests. Si no existe cobertura automatizada aplicable, declarar la limitación y la evidencia alternativa; no omitir pruebas necesarias de migraciones, pérdida de datos, permisos o regresiones corregidas.

## Regresión GitHub por lote aceptado

Ejecutar una vez sobre la versión final del lote aceptado, antes de generar un APK para uso habitual o cerrar los issues del lote:

```powershell
.\gradlew.bat :app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug
```

Añadir los checks de otros módulos afectados. No repetir esta batería por cada ajuste ni por el mero acto de hacer un commit. Una compilación focalizada permite entregar **listo para revisión**, no afirmar **validado para uso habitual** ni cerrar el lote.

Reutilizar evidencia satisfactoria de la misma versión del árbol, entorno y alcance, incluida CI, sin duplicar comandos. Cambios posteriores que afecten el resultado invalidan esa evidencia. Usar caché e incrementalidad; no ejecutar `clean`, resolver dependencias ni instalar herramientas por rutina.

**G4 — revisión:** revisión directa del diff y riesgos afectados. Revisores especializados solo cuando sean necesarios o se soliciten. La revisión manual complementa, no sustituye, la validación automatizada requerida.

**G5 — entrega:** reporte breve con alcance, versión/estado del árbol, comandos y resultados reales, checks no ejecutados y motivo, riesgos y aceptación humana. Un check requerido fallido, omitido o bloqueado impide aprobar esa etapa: reportar BLOCKED/incompleto. Un check fuera de alcance es **No requerido**, no PASS ni bloqueo.

## Ediciones y CI

GitHub es el flavor predeterminado. Mantener el código Play, pero ejecutar sus checks y builds **solo por solicitud explícita del propietario**, incluso si el cambio es compartido. Declarar Play no verificado cuando no se solicite.

```powershell
.\gradlew.bat :app:lintPlayDebug :app:testPlayDebugUnitTest :app:assemblePlayDebug
```

En Linux/macOS usar `./gradlew` para los mismos comandos.

- `init.ps1` / `init.sh` solo consultan el estado local por defecto, sin dependencias, lint, tests ni build. `RUN_START_COMMAND=1` conserva la instalación explícita en el dispositivo.
- CI ejecuta regresión GitHub en pushes a `releases` o manualmente, no en cada push/PR de desarrollo.
- La ejecución manual permite solicitar Play (`include_play`) y un build firmado (`build_release`); ambos desactivados por defecto. Solo `releases` publica, después de checks satisfactorios.
- Los commits, merges, cierres de issues y pushes siguen requiriendo la autorización correspondiente del propietario.
