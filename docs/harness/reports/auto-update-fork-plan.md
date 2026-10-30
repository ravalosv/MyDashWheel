# Plan: reactivar la autoactualización apuntando a los releases del fork

**Fecha:** 2026-10-29
**Estado:** Autorizada (aprobada el 2026-10-29)

**GitHub Ticket:** [#10](https://github.com/ravalosv/MyDashWheel/issues/10) en `ravalosv/MyDashWheel`

---

## Contexto

La comprobación automática de actualizaciones se desactivó a mano en `AutomotiveDashboard.kt` con el
comentario "Auto-update checks disabled for now (local builds / custom branding)". Quedaron comentados:
el bucle de comprobación, el bloque de auto-descarga y aviso "Update ready", y la constante
`UPDATE_CHECK_EVERY_MS`. El chequeo manual (⋮ → "Update to vX") siguió funcionando.

Entre tanto, el rebrand (ticket #6) dejó `UpdateManager` leyendo `BuildConfig.GITHUB_OWNER` =
`ravalosv` y `GITHUB_REPO` = `MyDashWheel`, y el ticket #9 hizo que solo un merge a `releases`
publique. El propietario pide reactivar la actualización automática contra esos releases.

## Decisión

Restaurar los tres bloques **tal como estaban escritos**, sin tocar la lógica: el diseño original ya
resolvía los casos que importan y cualquier cambio de comportamiento ahora sería una conjetura.

1. Bucle: comprueba al arrancar, en cada encendido (`ignitionFlow().drop(1)`) y cada 6 h despierto;
   solo con `Edition.full` (la edición Play la actualiza Google Play).
2. Auto-descarga: solo si hay permiso de instalación y la red no es de pago, una vez por build.
3. Aviso "Update ready": solo aparcado, una vez por build, y cede el paso a las notas del release.

## Verificaciones previas

| Comprobación | Resultado |
|---|---|
| `UpdateManager.isOurUpdate` exige `packageName == context.packageName` y firmas coincidentes | ✅ el cambio de `applicationId` del rebrand no lo rompe: el APK del fork se instala sobre sí mismo |
| `isMainTag` acepta `^v\d+\.\d+\.\d+$` | ✅ los tags nuevos son `v1.0.<n>` |
| Canal estable lee `/releases/latest` | ✅ ignora los pre-releases hasta que `promote.yml` los promueva |
| Canal beta recorre `/releases` y filtra por `isMainTag` | ✅ toma las betas de `releases` |
| Asset elegido: primer `.apk` que no sea companion ni edición Play | ✅ `pickLauncherApk` |
| URL consultada | `https://api.github.com/repos/ravalosv/MyDashWheel/releases` |

## Cambios

`app/src/main/java/com/openauto/dash/AutomotiveDashboard.kt`:

- `LaunchedEffect` con `Edition.full` que comprueba al arrancar, en cada encendido y cada 6 h.
- `autoDownloadedBuild`, `promptedBuild` y el `LaunchedEffect(updateStatus, moving)` de auto-descarga
  y aviso.
- `UPDATE_CHECK_EVERY_MS = 6 * 60 * 60_000L`.

Los imports necesarios (`delay`, `launch`, `mutableLongStateOf`, `ignitionFlow`) ya estaban en el
archivo, así que no se añade ninguno.

## Criterios de aceptación

- [ ] Compila sin símbolos huérfanos ni imports sin usar.
- [ ] `lintGithubDebug` sin errores y `testGithubDebugUnitTest` en verde.
- [ ] Con la edición GitHub arranca el chequeo al iniciar; en la edición Play no.
- [ ] El aviso no aparece con el coche en marcha ni dos veces para el mismo build.

## Riesgos

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Un release con la clave equivocada no instala encima | Bajo: el updater lo detecta y no lo ofrece | `isOurUpdate` compara firmas y paquete |
| El chequeo cada 6 h consume datos | Bajo | La descarga solo ocurre en red no medida y con permiso de instalación |
| La primera actualización real no se ha probado en dispositivo | Medio | Verificar con el primer release de `releases` instalado a mano sobre una versión anterior |

## Estado de Aprobación

- [x] Autorización para implementar: "Hay que volverlo a activar pero esta vez apuntando a los releases
      de mi repositorio"
- [x] Ticket GitHub creado: [#10](https://github.com/ravalosv/MyDashWheel/issues/10)
