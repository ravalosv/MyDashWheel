# Plan: publicar releases solo al fusionar en la rama `releases`

**Fecha:** 2026-10-29
**Estado:** Autorizada (aprobada el 2026-10-29)

**GitHub Ticket:** [#9](https://github.com/ravalosv/MyDashWheel/issues/9) en `ravalosv/MyDashWheel`

---

## Contexto

`build.yml` separa el trabajo en tres jobs: `checks` (lint + tests), `debug-build` (solo PRs) y
`release` (firma y publica). Hoy `release` corre en **cualquier** push que no sea PR, y publica la
pre-release beta cuando la rama es `main`. Publicar acaba siendo un efecto secundario de cada commit,
y cualquier rama de trabajo produce un pre-release suelto.

Solicitud del propietario: que el workflow solo publique al fusionar en una rama `releases`.

## Decisión

Gatear la **publicación**, no el workflow entero. Si el workflow completo dependiera de `releases`, los
PRs se quedarían sin lint ni tests, que es justo lo que evita fusionar código roto.

| Evento | Qué corre |
|---|---|
| push a `main` o cualquier rama | `checks` |
| PR a `main` | `checks` + `debug-build` |
| merge `main` → `releases` | build firmado + pre-release beta |
| `workflow_dispatch` | release a mano, desde la rama que se elija |

## Cambios

1. Job `release`: añadir `if: github.event_name == 'workflow_dispatch' || github.ref == 'refs/heads/releases'`.
2. Paso de notas del release: `refs/heads/main` → `refs/heads/releases`.
3. Paso `softprops/action-gh-release` del beta: `refs/heads/main` → `refs/heads/releases`.
4. Paso del pre-release de rama: su condición (`!= 'refs/heads/main'`) se vuelve redundante con el gate
   del job; se retira para no publicar dos releases en el mismo run.

## Consecuencias

- Los pre-releases de rama desaparecen. Para probar una rama en el head unit queda
  `workflow_dispatch`, o fusionarla en `releases`.
- El `versionCode` sigue siendo monótono: la fórmula cuenta los commits de la historia de la rama
  (`git rev-list --count --first-parent HEAD + 200`) y la rama `releases` hereda la de `main` al nacer.
- `promote.yml` no cambia: promueve por tag, sin importar la rama de origen.
- La rama `releases` creada desde `main` queda en el mismo commit, así que el primer merge es
  fast-forward y el primer release sale con el número siguiente al último publicado.

## Criterios de aceptación

- [ ] Push a `main`: solo `checks`, sin firma ni release.
- [ ] Push a `releases`: pre-release beta con `MyDashwheel-<v>.apk` y `mydashwheel-companion.apk`.
- [ ] PR a `main`: lint, tests y debug build.
- [ ] `workflow_dispatch` sigue publicado a mano.
- [ ] `promote.yml` sin cambios.

## Riesgos

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Olvidar fusionar en `releases` | Bajo: nada se publica | Es el objetivo: publicar deja de ser automático |
| El gate deja el job `release` en "skipped" en cada push a `main` | Ninguno funcional, ruido visual en Actions | Aceptado |
| La rama `releases` recibe un commit directo sin pasar por `main` | Medio: release sin los checks de un PR | Proteger la rama: solo merges |

## Estado de Aprobación

- [x] Revisión de spec: aprobada por el propietario el 2026-10-29 ("hazlo como los sugieres")
- [x] Autorización para implementar: la misma
- [x] Ticket GitHub creado: [#9](https://github.com/ravalosv/MyDashWheel/issues/9)
