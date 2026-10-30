# Plan: Rebrand a MyDashwheel (nombre de release, applicationId y publicación propia)

**Fecha:** 2026-10-29
**Estado:** Autorizada (aprobada el 2026-10-29)

**GitHub Ticket:** [#6](https://github.com/ravalosv/MyDashWheel/issues/6) en `ravalosv/MyDashWheel`

---

## Contexto

El proyecto es un fork de `deviloufr-ai/Dashwheel`. Todo lo que publica lleva el nombre de upstream
(`Dashwheel-<version>.apk`, títulos "Dashwheel v…"), el `applicationId` es el de upstream
(`com.openauto.dash`) y el updater in-app consulta los releases de **upstream**, no los del fork.

Solicitud del propietario:
1. El release debe llamarse **MyDashwheel**.
2. Debe poder **generarse y publicarse** en su repositorio de GitHub.
3. `applicationId` debe ser **`com.mydashwheel.dash`**.

## Requisitos

| ID | Requisito |
|----|-----------|
| RF-01 | Los assets del release se llaman `MyDashwheel-<version>.apk` y `mydashwheel-companion.apk` |
| RF-02 | Los títulos de release y de promoción dicen MyDashwheel |
| RF-03 | `applicationId`: `com.mydashwheel.dash` (github), `com.mydashwheel.dash.play` (play) |
| RF-04 | El companion: `com.mydashwheel.dash.companion` y `com.mydashwheel.dash.companion.play` |
| RF-05 | Ninguna cadena con el id antiguo sobrevive donde se refiere a la propia app |
| RF-06 | El updater in-app consulta `ravalosv/MyDashWheel` |
| RF-07 | Existen los cuatro secretos de firma en el repo para que CI pueda firmar |
| RF-08 | La etiqueta visible (lanzador) dice MyDashwheel / MyDashwheel Companion en los 11 idiomas |

## Decisiones

- **`namespace` y paquetes Kotlin no cambian** (`com.openauto.dash`). Cambiarlos obliga a mover cientos
  de archivos y a reescribir todos los `import …R`; el identificador que ven Android y Play es el
  `applicationId`, que es lo que se cambia.
- **Las cadenas que sí son identidad de la app pasan a `BuildConfig.APPLICATION_ID`**, para que un
  cambio futuro de id no las deje obsoletas: `PrivApp` (whitelist priv-app, instalación en
  `/system/priv-app`, dexopt), `SystemDisplayHelper` (paquete por defecto) y los `selfPackage` de
  `WindowListing`.
- **Se conservan** las cadenas que son API o identidad ajena: prefijos de `Automation`
  (`com.openauto.dash.action.*`, documentados en `docs/automation.md`), extras de `MainActivity`,
  acciones de `SystemDisplayHelper`, `USB_OBD_PERMISSION` y el paquete del overlay de volumen
  (`com.openauto.dash.volumeoverlay`, que es otro APK y se construye aparte).

## Incrementos

### 1. Identidad
- [x] `app`: `applicationId` → `com.mydashwheel.dash`; flavor play → `com.mydashwheel.dash.play`
- [x] `companion`: id → `com.mydashwheel.dash.companion`; play → `…companion.play`
- [x] `BuildConfig.GITHUB_OWNER`/`GITHUB_REPO` → `ravalosv` / `MyDashWheel`
- [x] Etiqueta visible: `app_name` de `app` → `MyDashwheel` (`translatable="false"`); `app_name` del
      `companion` → `MyDashwheel Companion` y sus 10 traducciones (`MyDashwheel Begleiter`,
      `… Compañero`, `… Compagnon`, `… Companheiro`, y `… Companion` en el resto)

### 2. Cadenas con el id escrito a mano
- [x] `PrivApp.PRIVAPP_XML` y `APP_PACKAGE` → `BuildConfig.APPLICATION_ID`
- [x] `SystemDisplayHelper` paquete por defecto → `BuildConfig.APPLICATION_ID`
- [x] `WindowListing` `selfPackage` por defecto (4) → `BuildConfig.APPLICATION_ID`
- [x] `adb … am start -n` de `.vscode/launch.json` y `tasks.json` → nuevo id

### 3. Nombres de release
- [x] `build.yml`: assets, títulos de beta y de pre-release de rama
- [x] `promote.yml`: título al promover a estable
- [x] `UpdateManager.COMPANION_APK_NAME` → `mydashwheel-companion.apk` (la URL de `PhoneLinkUi` sigue la constante)
- [x] `UpdateManagerTest` actualizado a los nombres nuevos

### 4. Publicación
- [x] Cargar los secretos `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` en el repo
      (hecho por el propietario el 2026-10-29; `gh secret list --repo ravalosv/MyDashWheel` muestra los cuatro)
- [x] Validación local del camino de CI: `:app:assembleGithubRelease` + `:companion:assembleGithubRelease`
      con las cuatro variables de entorno y **sin** `keys/keystore.properties` presente → `BUILD SUCCESSFUL`,
      ambos APK firmados con `SHA-256 7dbc6c4a…be44` (`CN=Dashwheel, O=Dashwheel Personal, C=MX`),
      sin el aviso de firma con la clave de debug
- [x] Validación en CI real: `gh workflow run build.yml` (run `40955976901`, `success`). Los APK del
      pre-release `v1.0.871` verifican con `SHA-256 7dbc6c4a…be44` y `CN=Dashwheel, O=Dashwheel Personal`,
      es decir, GitHub inyecta bien los cuatro secretos y `KEYSTORE_BASE64` decodifica. Ese run usó `main`
      antes del rebrand: publica `Dashwheel-1.0.871.apk` con `com.openauto.dash`, como se esperaba

### 5. Validación
- [x] `:app:lintGithubDebug` (0 errores), `:app:testGithubDebugUnitTest`, `:app:assembleGithubDebug`,
      `:companion:assembleGithubDebug`
- [x] `aapt2 dump badging`: `package name='com.mydashwheel.dash'` y `'com.mydashwheel.dash.companion'`;
      mismo id en el APK de release firmado, con `application-label:'MyDashwheel'` y
      `'MyDashwheel Companion'`
- [ ] Instalación y arranque en dispositivo (`adb install -r`, nombre del lanzador)

## Pendiente de decisión

- _(ninguno)_ La etiqueta visible la incluyó el propietario en el alcance de este ticket el 2026-10-29.

## Riesgos

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Cambiar el id rompe la app como priv-app | Alto | `PRIVAPP_XML` y `APP_PACKAGE` ya derivan del build; probar `installAsSystemApp` en unidad |
| Automatizaciones externas dejan de funcionar | Medio | Los prefijos de `Automation` se conservan a propósito |
| Un id nuevo no actualiza la app instalada anterior | Medio | Esperado: se instala al lado. Desinstalar la versión de upstream |
| Los releases del fork no llegan a la app ya instalada | Alto | `GITHUB_OWNER`/`GITHUB_REPO` pasan a apuntar al fork |

## Estado de Aprobación

- [x] Revisión de spec: aprobada por el propietario el 2026-10-29
- [x] Autorización para implementar: "hay que modificar para que en lugar de Dashwheel el nombre del release sea MyDashwheel. Hay que habilitar para que se pueda generar y publicar el release en github. Cambiar tambien el packagename a com.mydashwheel.dash"
- [x] Ticket GitHub creado: [#6](https://github.com/ravalosv/MyDashWheel/issues/6)
- [x] Commits: `ddc4035f` (rebrand) y `16c3f36d` (restauración de `gradlew.bat`) en
      `feat/rebrand-mydashwheel`; fusionados en `main` por PR #7 (`94be5f43`) y PR #8 (`aeeb44cd`)
- [x] Ticket cerrado el 2026-10-29 con el comentario de evidencia
