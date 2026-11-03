# Harness de Dashwheel

Este directorio contiene el sistema de trabajo (harness) para desarrollo con agentes de IA, basado en la metodología de [Learn Harness Engineering](https://walkinglabs.github.io/learn-harness-engineering/es/).

## Estructura

```
harness/
├── quality-gates.md              # Puertas de calidad (G0-G5)
├── workflows/
│   ├── implement-story.md        # Flujo de implementación
│   └── prepare-story.md          # Flujo de preparación
├── checklists/
│   ├── definition-of-done.md     # Criterios de completado
│   └── security-review.md        # Revisión de seguridad
├── templates/
│   ├── implementation-report.md  # Reporte de implementación
│   └── story-plan.md             # Plan de issue
└── reports/                      # Reportes de issues específicos
```

## Archivos raíz relacionados

- `AGENTS.md` - Instrucciones para agentes de codificación
- `init.sh` / `init.ps1` - Consulta del estado local, sin Gradle por defecto
- `claude-progress.md` - Registro de progreso entre sesiones
- `feature_list.json` - Lista de características y su estado

## Configuración de Claude Code

El directorio `.claude/` en la raíz del repositorio contiene:

```
.claude/
├── settings.example.json         # Configuración de permisos
├── agents/
│   ├── architecture-reviewer.md  # Revisor de arquitectura
│   ├── security-reviewer.md      # Revisor de seguridad
│   └── test-reviewer.md          # Revisor de tests
└── commands/
    ├── adr-proposal.md           # Proponer decisión arquitectónica
    ├── implement-story.md        # Implementar issue
    ├── prepare-story.md          # Preparar issue
    └── review-story.md           # Revisar issue
```

## Uso

### Para agentes

1. Lee `AGENTS.md` antes de trabajar
2. Consulta el estado local con `./init.sh` (o `.\init.ps1` en Windows), sin Gradle por defecto
3. Revisa `docs/harness/quality-gates.md` antes de proponer un plan
4. Usa los workflows en `docs/harness/workflows/` como guía
5. Reporta brevemente evidencia en el issue o conversación; usa archivos solo si se solicitan o son necesarios
6. Actualiza el progreso cuando aporte información nueva, no por rutina

### Comandos de verificación

La política canónica está en [quality-gates.md](quality-gates.md). Durante desarrollo, validar por riesgo con compilación y tests focalizados. Documentación no requiere Gradle; scripts del harness requieren validación propia. No usar `clean` ni resolver dependencias por rutina; caché e incrementalidad ya están habilitadas.

**GitHub: una regresión por lote aceptado, antes de APK para uso habitual o cierre:**
```bash
./gradlew :app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug
```

Añadir módulos afectados y reutilizar evidencia válida de la misma versión, entorno y alcance. Los checks focalizados permiten entregar listo para revisión, no validado para uso habitual.

**Play: solo por solicitud explícita del propietario:**
```bash
./gradlew :app:lintPlayDebug :app:testPlayDebugUnitTest :app:assemblePlayDebug
```

En Windows usar `.\gradlew.bat`. Mantener el código Play y declarar cuando no se verificó.

### Inicio y CI

- `init.ps1` / `init.sh` no instalan dependencias ni ejecutan lint, tests o build por defecto. `RUN_START_COMMAND=1` conserva la instalación explícita en un dispositivo.
- CI solo se ejecuta en pushes a `releases` o manualmente; no en cada push/PR de desarrollo.
- La ejecución manual valida GitHub y genera APKs debug. `include_play` añade checks y build Play; `build_release` solicita el build firmado. Ambos son opcionales y desactivados por defecto.
- Los pushes a `releases` mantienen el firmado y publicación beta después de los checks. Una ejecución manual fuera de `releases` no publica.

### Comandos de Claude Code

- `/adr-proposal <tema>` - Proponer una decisión arquitectónica
- `/prepare-story <issue>` - Preparar especificación para un issue
- `/implement-story <issue>` - Implementar un issue
- `/review-story <issue>` - Revisar implementación de un issue

## Adaptación a Dashwheel

Este harness está adaptado para:
- **Lenguaje:** Kotlin + Jetpack Compose
- **Build system:** Gradle con multi-módulo y product flavors
- **Tracker:** GitHub Issues (`gh` CLI)
- **Arquitectura:** Android multi-module (`:app`, `:companion`, `:display`, `:link`, `:unitproto`, `:fakeunit`)
- **Compatibilidad:** Android 9+ (minSdk 28), ediciones github/play
- **Verificación:** Comandos específicos por flavor (ver sección "Comandos de verificación")

## Referencias

- [Learn Harness Engineering](https://walkinglabs.github.io/learn-harness-engineering/es/)
- [Anthropic: Effective harnesses for long-running agents](https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents)
- [OpenAI: Harness engineering](https://openai.com/index/harness-engineering/)
