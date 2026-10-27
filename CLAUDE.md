## Agent skills

### Issue tracker

Issues live in GitHub Issues of deviloufr-ai/Dashwheel (public, testers read it), via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five default roles, each label named as its role (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

## Harness Engineering

Este repositorio usa un sistema de trabajo (harness) para desarrollo con agentes de IA, basado en [Learn Harness Engineering](https://walkinglabs.github.io/learn-harness-engineering/es/).

### Antes de trabajar

1. Lee `AGENTS.md` para instrucciones completas
2. Ejecuta `./init.sh` (o `./init.ps1` en Windows) para verificar el estado base
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

Ejecuta siempre antes de entregar (edición GitHub):
```bash
./gradlew :app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug
```

Para verificar también la edición Play:
```bash
./gradlew :app:lintPlayDebug :app:testPlayDebugUnitTest :app:assemblePlayDebug
```

Los commits permanecen locales hasta autorización explícita del propietario.
