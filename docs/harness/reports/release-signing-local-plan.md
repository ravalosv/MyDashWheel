# Plan: Firma de release con claves propias (keystore local)

**Fecha:** 2026-10-29
**Estado:** Autorizada (aprobada el 2026-10-29)

**GitHub Ticket:** [#3](https://github.com/ravalosv/MyDashWheel/issues/3) en `ravalosv/MyDashWheel`

---

## Contexto

`app/build.gradle.kts` y `companion/build.gradle.kts` leen la firma de release solo de variables de
entorno (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), que en CI vienen de los
secretos `KEYSTORE_BASE64`, etc. En local no hay keystore, así que `release` cae a la clave de debug:
el APK compila, pero no puede actualizar in-place nada firmado con la clave real.

`.gitignore` ya excluye `*.jks`, `*.keystore`, `*.p12` y `keystore.properties`.

## Requisitos

| ID | Requisito |
|----|-----------|
| RF-01 | El release local se firma con un keystore propio, sin variables de entorno |
| RF-02 | Si no hay keystore ni `keystore.properties`, se crea uno (proceso documentado y reproducible) |
| RF-03 | CI sigue funcionando igual: env vars primero y aborta en release si no hay keystore |
| RF-04 | El keystore y su contraseña nunca entran al repositorio |
| RF-05 | El comportamiento de debug no cambia |

## Diseño

Orden de resolución de la firma:

1. `KEYSTORE_FILE` (y las demás env vars) — CI, sin cambios.
2. `keys/keystore.properties` en el repo — local:
   ```properties
   storeFile=keys/release.keystore
   storePassword=...
   keyAlias=dashwheel
   keyPassword=...
   ```
3. Nada de lo anterior → se firma con la clave de debug, como hasta ahora.

El código se centraliza en `gradle/release-signing.gradle.kts`, aplicado desde los dos módulos, en
lugar de duplicar la lectura en cada uno.

## Incrementos

### 1. Script compartido
- [x] `gradle/release-signing.gradle.kts`: resuelve env vars → `keys/keystore.properties` → ninguno
- [x] Expone el resultado como extra properties para los módulos

### 2. Módulos
- [x] `app/build.gradle.kts` aplica el script y usa la firma resuelta
- [x] `companion/build.gradle.kts` hace lo mismo

### 3. Claves
- [x] Generar `keys/release.keystore` con `keytool` (RSA 4096, 10000 días, alias `dashwheel`)
- [x] Crear `keys/keystore.properties` con contraseña aleatoria
- [x] Verificar que ambos quedan ignorados por git

### 4. Validación
- [x] `:app:assembleGithubRelease` compila y firma
- [x] `apksigner verify` confirma la firma propia
- [x] `:app:assembleGithubDebug` sigue funcionando

**Validación ejecutada (2026-10-29):**

| Comprobación | Resultado |
|---|---|
| `:app:tasks :companion:tasks` | configuración OK (el script aplica en ambos módulos) |
| `:app:signingReport` | `githubRelease` y `playRelease` → `keys/release.keystore`, alias `dashwheel` |
| `:app:assembleGithubRelease :companion:assembleGithubRelease` | **BUILD SUCCESSFUL** (10m 52s); APK 48 MB y 36 MB |
| `apksigner verify --print-certs` | `CN=Dashwheel, O=Dashwheel Personal, C=MX`, SHA-256 `7dbc6c4a…cbe44` |
| Sin `keys/keystore.properties` → `signingReport` | cae a `debug.keystore` / `AndroidDebugKey` (RF-05) |
| `:app:assembleGithubDebug` | **BUILD SUCCESSFUL**, 4s |
| `git check-ignore keys/release.keystore keys/keystore.properties` | ambos ignorados (RF-04) |

**Incidencias corregidas:**
1. `ExtraPropertiesExtension.find(...)` no existe en Kotlin DSL → `extra.has(...)`.
2. El script escribía las extra properties en el proyecto del módulo, pero los módulos las leían de
   `rootProject.extra` → se leen del propio módulo (el script se aplica por módulo).

**Clave generada:**
`keys/release.keystore` — RSA 4096, alias `dashwheel`, PKCS12, válida hasta 2054-03-16.
Contraseña en `keys/keystore.properties` (fuera del repositorio). **Respaldar ambos fuera del equipo.**

**Pendiente:** commit del cambio (requiere autorización del propietario) y cierre del ticket #3.

**Icono de aviso (2026-10-29): la versión no se incrementa en local.** `assembleGithubRelease` sin
`VERSION_CODE`/`VERSION_NAME` produce `versionCode=1 / versionName=1.0`; solo CI calcula el código
(commits de `main` + 200). Un APK así no instala encima de ninguna release real
(`INSTALL_FAILED_VERSION_DOWNGRADE`). Propuesta para un ticket aparte: que el build local use la misma
fórmula que CI, dejando las env vars como override.

## Validación de la firma en CI (secretos del repositorio)

**Ejecutada el 2026-10-29.** Local firma con `keys/keystore.properties`, pero CI firma con los cuatro
secretos del repositorio. Que un build local funcione **no** prueba que los secretos estén bien: hay que
validarlos en tres niveles. Los comandos son PowerShell, desde la raíz del proyecto.

Huella de referencia del certificado propio (la que debe aparecer siempre):

```
SHA-256: 7dbc6c4a5c24d7374307bdcf4d4cb481da6232f7789a60a5802df5d4ddfcbe44
DN:      CN=Dashwheel, O=Dashwheel Personal, C=MX  (RSA 4096, válido hasta 2054)
```

### Nivel 1 — los secretos existen en el repo correcto

```powershell
gh auth status                                     # la cuenta activa debe ser ravalosv
gh secret list --repo ravalosv/MyDashWheel
```

Deben aparecer `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` y `KEY_PASSWORD`. **Solo prueba que
existen**, no que el contenido sea correcto: un base64 mal formado o un alias equivocado falla recién al
compilar.

### Nivel 2 — el camino de variables de entorno, en local

Simula CI: firma usando **solo** las cuatro variables, con `keys/keystore.properties` fuera de su ruta.
No publica nada y tarda ~6 min.

```powershell
$props = @{}
Get-Content keys\keystore.properties | ForEach-Object {
  if ($_ -match '^\s*([^#=]+)=(.*)$') { $props[$matches[1].Trim()] = $matches[2].Trim() }
}
$env:KEYSTORE_FILE     = (Resolve-Path $props['storeFile']).Path
$env:KEYSTORE_PASSWORD = $props['storePassword']
$env:KEY_ALIAS         = $props['keyAlias']
$env:KEY_PASSWORD      = $props['keyPassword']

# Rutas explícitas a propósito: ver "Errores de uso" más abajo.
Move-Item .\keys\keystore.properties .\keystore.properties.bak
try {
  .\gradlew.bat :app:assembleGithubRelease :companion:assembleGithubRelease
  "gradle exit = $LASTEXITCODE"
} finally {
  Move-Item .\keystore.properties.bak .\keys\keystore.properties
}
```

Verificar la firma (un APK por invocación):

```powershell
foreach ($f in Get-ChildItem app\build\outputs\apk\github\release\*.apk,
                           companion\build\outputs\apk\github\release\*.apk) {
  Write-Host "== $($f.Name)"
  & "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --print-certs $f.FullName
}
```

**Resultado:** `BUILD SUCCESSFUL` (6m 1s), ambos APK con la huella de referencia y sin el aviso
`Release signed with the DEBUG key`. Detecta alias o contraseñas equivocadas sin gastar CI.

### Nivel 3 — CI real (publica un pre-release)

```powershell
gh workflow run build.yml --repo ravalosv/MyDashWheel --ref main
gh run list --repo ravalosv/MyDashWheel --workflow build.yml --limit 1
gh run watch --repo ravalosv/MyDashWheel --exit-status

$tag = gh release list --repo ravalosv/MyDashWheel --limit 1 --json tagName -q '.[0].tagName'
gh release download $tag --repo ravalosv/MyDashWheel --pattern "*.apk" --dir .\verify --clobber

foreach ($f in Get-ChildItem .\verify\*.apk) {
  Write-Host "== $($f.Name)"
  & "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --print-certs $f.FullName
  & "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\aapt2.exe" dump badging $f.FullName |
      Select-String '^package: name|^application-label:'
}
```

**Resultado:** run `40955976901` → `success`, pre-release `v1.0.871`. Los dos APK verifican con la huella
de referencia, así que GitHub inyecta bien los secretos y `KEYSTORE_BASE64` decodifica correctamente.
Ese run compiló `main` antes del rebrand, por lo que publica `Dashwheel-1.0.871.apk` con
`com.openauto.dash` y `versionCode 871`: es lo esperado, no un fallo.

### Errores de uso encontrados (y su causa)

| Síntoma | Causa | Solución |
|---|---|---|
| `Rename-Item : No se puede cambiar el nombre porque el elemento situado en 'keystore.properties.bak' no existe` | En PowerShell, `Rename-Item keys\archivo keystore.properties.bak` resuelve `NewName` **relativo a la carpeta del propio elemento**: el archivo acabó en `keys\keystore.properties.bak`, no en la raíz | Usar `Move-Item` con ruta de origen y destino completas (bloque del Nivel 2). Si ya ocurrió: `Move-Item .\keys\keystore.properties.bak .\keys\keystore.properties` |
| `Unexpected parameter(s) after APK (verify\dashwheel-companion.apk)` | `apksigner` acepta **un solo** APK por invocación; el comodín se expandió a dos rutas | Recorrer con `foreach` (bloques de Nivel 2 y 3) |

## Riesgos

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Perder el keystore o la contraseña | Alto: nunca se podrá actualizar la app publicada | Respaldar `keys/release.keystore` + `keys/keystore.properties` fuera del repo (gestor de contraseñas / copia cifrada) |
| Clave nueva ≠ clave de la app ya instalada | Medio: hay que desinstalar la versión anterior | Esperado: es una clave propia, no la de las releases de upstream |
| `keystore.properties` con ruta absoluta al commiteares | Alto: rompe en otra máquina | Ruta relativa a la raíz; el archivo está en `.gitignore` |

## Estado de Aprobación

- [x] Revisión de spec: aprobada por el propietario el 2026-10-29
- [x] Autorización para implementar: "hay que modificar el proyecto para que compile en release con mis propias KEYS, si no existen hay que crearlas"
- [x] Ticket GitHub creado: [#3](https://github.com/ravalosv/MyDashWheel/issues/3)
