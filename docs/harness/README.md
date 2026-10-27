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
- `init.sh` / `init.ps1` - Scripts de inicialización y verificación
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
2. Ejecuta `./init.sh` (o `./init.ps1` en Windows) para verificar el estado base
3. Revisa `docs/harness/quality-gates.md` antes de proponer un plan
4. Usa los workflows en `docs/harness/workflows/` como guía
5. Registra evidencia en `docs/harness/reports/`
6. Actualiza `claude-progress.md` al final de cada sesión

### Comandos de verificación

El proyecto tiene dos product flavors (`github` y `play`). El flavor por defecto es **`github`**.

**Edición GitHub (default, siempre verificar):**
```bash
./gradlew :app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug
```

**Edición Play (solo cuando el cambio afecte esa edición o código compartido):**
```bash
./gradlew :app:lintPlayDebug :app:testPlayDebugUnitTest :app:assemblePlayDebug
```

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
