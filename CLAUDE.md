# Reglas del Harness - Proyecto Dashwheel

## REGLA DE TICKETS GITHUB
1. **Toda solicitud de implementación de código requiere un ticket en GitHub**
   - **Incluye**: Corrección de bugs, nuevas funcionalidades, cualquier cambio que modifique código fuente
   - **Excluye**: Configuración de harness, documentación interna (como este archivo), análisis sin implementación
2. **El ticket debe crearse en el repositorio PERSONAL: `https://github.com/ravalosv/MyDashWheel.git`**
3. **NO usar el repositorio upstream (`deviloufr-ai/Dashwheel`)**
4. **Toda implementación de código debe referenciar el número de ticket**
5. **Sin número de ticket → NO proceder con implementación de código**
6. **Configuración del harness/documentación interna NO requiere ticket**
## REGLA DE RAMAS Y COMMITS
**Todo commit y toda rama nueva se crean a partir de `main`, siempre, salvo que el propietario
indique otra base explícitamente.**

1. **Antes de tocar nada, comprobar la rama:** `git rev-parse --abbrev-ref HEAD`.
   Si no es `main`, cambiar a `main` y actualizarla (`git fetch origin && git reset --hard origin/main`).
2. **Nunca commitear sobre la rama en la que se estaba "por casualidad"** (`releases`, una rama de
   feature abandonada, un checkout suelto). El commit se pierde del flujo o, peor, acaba donde no toca.
3. **Una rama de trabajo nace de `main`**: `git switch -c <tipo>/#<ticket>-<slug> main`.
4. **`main` es la única fuente**: `releases` solo recibe merges desde `main`; ninguna rama de trabajo
   se crea a partir de `releases`.
5. **Si un commit cae en la rama equivocada**: `git cherry-pick <sha>` sobre `main` y realinear la
   otra rama con `origin/<rama>`. No se reescribe historia ya publicada.
6. **Antes de hacer push, verificar las refs**: `git branch -vv` debe mostrar la rama y el remoto que
   se esperan. Un `git reset --hard` cruzado deja `main` y `releases` invertidas y no avisa.

**Excepción**: solo cuando el propietario pida explícitamente partir de otra rama (por ejemplo, un
hotfix sobre un tag publicado), y en ese caso se anota en el reporte del ticket.

## REGLA DE DESACOPLAMIENTO DEL FORK
**Este repositorio es un fork de `deviloufr-ai/Dashwheel`, que sigue recibiendo actualizaciones. Todo
cambio propio se aísla del código heredado en la medida de lo posible, para que los merges del
upstream no produzcan conflictos y para poder distinguir "mis cambios" de "el fork original".**

1. **Código propio en paquete propio.** Lo nuevo (arreglos, parches, comportamientos nuestros) vive en
   `com.openauto.dash.mine` (app) y equivalentes por módulo, en archivos nuevos, no repartido por los
   archivos heredados.
2. **En un archivo heredado, la huella es mínima.** Un arreglo que necesite engancharse al código del
   upstream deja una sola llamada de una línea a nuestra función (por ejemplo
   `MineOverrides.navLocationEngine(app)`). Nada de lógica propia dentro del archivo heredado.
3. **Señalizar lo tocado.** Cada línea añadida a un archivo heredado va marcada con `// MINE:` y el
   número de ticket, para encontrarlas con `grep -rn "MINE:" app/src/main` tras un merge.
4. **Nada de refactors cosméticos en archivos heredados.** Renombrar, reformatear o reordenar código
   del upstream solo crea conflictos; si hace falta, se hace en nuestro paquete.
5. **Si un arreglo no se puede aislar** (el fallo está dentro del upstream y exige cambiarlo), se
   documenta en el reporte del ticket el porqué y se deja el diff más pequeño que funcione, marcado
   con `// MINE:`.
6. **Ficheros de build y workflows**: añadir nuestro bloque al final, con un comentario que lo
   identifique; no reescribir los del upstream.
7. **Verificación tras un merge del upstream**: `grep -rn "MINE:"` debe seguir encontrando cada
   punto de contacto; si desaparece alguno, el merge se llevó un arreglo nuestro y hay que reponerlo.

## Autorización y planificación

Antes de implementar código de la app, confirmar ticket en `ravalosv/MyDashWheel`,
autorización explícita y decisiones bloqueantes resueltas para ese issue.
No usar el estado de un plan histórico de otro issue como bloqueo global.
Configuración del harness y documentación interna no requieren ticket, pero sí autorización para editar.
Análisis y lectura no requieren autorización de implementación.
Los checks requeridos por [quality-gates.md](docs/harness/quality-gates.md) forman parte
de la validación del cambio autorizado; no pedir permiso por cada comando.
Resumir contexto y pruebas en la conversación o issue. No crear planes, matrices o reportes
extensos por rutina ni repetir recordatorios de reglas en cada respuesta.

## Agent skills

### Issue tracker

Issues de este fork viven exclusivamente en `ravalosv/MyDashWheel`, mediante `gh`. Consulta `docs/agents/issue-tracker.md`.

### Triage labels

No se requieren etiquetas ni estados de triage del upstream. `docs/agents/triage-labels.md` es solo una referencia del upstream.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

## Harness Engineering

Este repositorio usa un sistema de trabajo (harness) para desarrollo con agentes de IA, basado en [Learn Harness Engineering](https://walkinglabs.github.io/learn-harness-engineering/es/).

### Antes de trabajar

1. Lee `AGENTS.md` para instrucciones completas
2. Consulta el estado local con `./init.sh` (o `.\init.ps1` en Windows); no ejecuta Gradle por defecto
3. Revisa `docs/harness/quality-gates.md` antes de proponer un plan
4. Consulta `claude-progress.md` para el estado actual del proyecto
5. Usa los workflows en `docs/harness/workflows/` como guía

### Comandos de Claude Code

- `/adr-proposal <tema>` - Proponer una decisión arquitectónica
- `/prepare-story <issue>` - Preparar especificación para un issue
- `/implement-story <issue>` - Implementar un issue
- `/review-story <issue>` - Revisar implementación de un issue

### Documentación del harness

- `docs/harness/README.md` - Guía completa del sistema
- `docs/harness/quality-gates.md` - Puertas de calidad (G0-G5)
- `docs/harness/workflows/` - Flujos de trabajo
- `docs/harness/checklists/` - Listas de verificación
- `docs/harness/templates/` - Plantillas para reportes y planes

### Verificación

La política canónica es [docs/harness/quality-gates.md](docs/harness/quality-gates.md).
Durante desarrollo, ejecutar compilación GitHub y tests focalizados por riesgo; ampliar el alcance
para migraciones, contratos, permisos, dependencias o merges upstream. Documentación no requiere
Gradle; los scripts del harness solo requieren validación propia.

```bash
./gradlew :app:compileGithubDebugKotlin
./gradlew :app:testGithubDebugUnitTest --tests "com.openauto.dash.<paquete>.<ClaseNueva>Test"
```

La regresión GitHub se ejecuta una vez por lote aceptado, antes de generar APK para uso habitual o
cerrar sus issues, no por cada ajuste ni por autorizar un commit. Los checks focalizados permiten
entregar listo para revisión; no permiten afirmar validado para uso habitual. Reutilizar evidencia
de la misma versión, entorno y alcance; no ejecutar `clean` ni resolver dependencias por rutina.

```bash
./gradlew :app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug
```

Play solo se verifica por solicitud explícita del propietario; conservar su código y declarar
cuando no se verificó:
```bash
./gradlew :app:lintPlayDebug :app:testPlayDebugUnitTest :app:assemblePlayDebug
```

CI solo ejecuta regresión en `releases` o manualmente, con Play opcional. Reportar brevemente
comandos/resultados reales y omisiones; checks requeridos fallidos u omitidos bloquean la etapa.
Los commits permanecen locales hasta autorización explícita del propietario.
