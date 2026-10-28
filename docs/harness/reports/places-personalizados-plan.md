# Plan: Lugares Personalizados en "Mis Lugares"

**Fecha:** 2026-10-27  
**Estado:** Propuesto (pendiente de aprobación)  

**Issue:** Feature solicitada por usuario  
**GitHub Ticket:** [#1](https://github.com/ravalosv/MyDashWheel/issues/1) en `ravalosv/MyDashWheel`

---

## Contexto

Actualmente la sección "Mis Lugares" ([`Places.kt`](app/src/main/java/com/openauto/dash/Places.kt), [`HandsFreeSettings.kt`](app/src/main/java/com/openauto/dash/HandsFreeSettings.kt)) solo permite configurar dos lugares fijos: Casa y Trabajo. 

**Solicitud del usuario:**
- Permitir agregar lugares personalizados (ej: Gimnasio, Casa de amigos, etc.)
- Permitir editar el nombre de cualquier lugar (incluyendo Casa y Trabajo)
- Permitir al usuario seleccionar qué lugar mostrar en el widget Home/Work

---

## Requisitos

### Funcionales (RF)

| ID | Requisito | Prioridad |
|----|-----------|-----------|
| RF-01 | El usuario puede agregar lugares personalizados ilimitados | P0 |
| RF-02 | Cada lugar tiene: ID único, nombre editable, coordenadas (lat/lng) | P0 |
| RF-03 | El usuario puede editar el nombre de cualquier lugar (Casa, Trabajo, personalizados) | P0 |
| RF-04 | El usuario puede eliminar lugares personalizados (no Casa/Trabajo) | P0 |
| RF-05 | El usuario puede seleccionar qué lugar mostrar en el widget Home/Work | P0 |
| RF-06 | Los lugares se persisten entre sesiones | P0 |
| RF-07 | Los datos legacy (sin customPlaces) se migran automáticamente | P0 |

### No funcionales (RNF)

| ID | Requisito |
|----|-----------|
| RNF-01 | Backward compatible con datos existentes |
| RNF-02 | UI coherente con el diseño actual del proyecto (DashColors, DashSpace, Material3) |
| RNF-03 | Soporte multi-idioma (al menos ES/EN) |

---

## Matriz: Requisito → Criterio → Prueba

| Requisito | Criterio de Aceptación | Prueba Manual |
|-----------|------------------------|---------------|
| RF-01 | Usuario puede agregar N lugares personalizados | MT-places-01 |
| RF-02 | Cada lugar guarda nombre, lat, lng, ID único | MT-places-02 |
| RF-03 | Nombre se edita y guarda inmediatamente | MT-places-03 |
| RF-04 | Lugar personalizado se elimina completamente | MT-places-04 |
| RF-05 | Widget muestra el lugar seleccionado | MT-places-05 |
| RF-06 | Datos persisten tras cerrar/reabrir app | MT-places-06 |
| RF-07 | Migración de versión anterior funciona sin errores | MT-places-07 |

---

## Casos de Prueba Manuales

### MT-places-01: Agregar lugar personalizado
- **Criterios cubiertos:** RF-01, RF-02
- **Precondiciones:** App instalada, GPS activado, usuario en Settings → Driving → Mis lugares
- **Pasos:**
  1. Tocar "Agregar lugar personalizado"
  2. Ingresar nombre "Gimnasio"
  3. Tocar "Buscar" o "El coche está aquí"
  4. Verificar que aparece en la lista de lugares personalizados
- **Resultado esperado:** "Gimnasio" aparece en la lista con coordenadas válidas

### MT-places-02: Editar nombre de lugar personalizado
- **Criterios cubiertos:** RF-02, RF-03
- **Precondiciones:** Tener al menos 1 lugar personalizado "Gimnasio"
- **Pasos:**
  1. Tocar ícono Editar en "Gimnasio"
  2. Cambiar nombre a "Gimnasio CrossFit"
  3. Tocar "Guardar"
- **Resultado esperado:** Nombre actualizado inmediatamente en la lista

### MT-places-03: Eliminar lugar personalizado
- **Criterios cubiertos:** RF-04
- **Precondiciones:** Tener lugar personalizado "Gimnasio"
- **Pasos:**
  1. Tocar ícono Eliminar en "Gimnasio"
  2. Confirmar eliminación (si aplica)
- **Resultado esperado:** "Gimnasio" desaparece de la lista

### MT-places-04: Seleccionar lugar para widget
- **Criterios cubiertos:** RF-05
- **Precondiciones:** Tener Casa, Trabajo y "Gimnasio" configurados
- **Pasos:**
  1. En dropdown "Mostrar en widget", seleccionar "Gimnasio"
  2. Regresar al dashboard
  3. Verificar widget Home/Work
- **Resultado esperado:** Widget muestra "Gimnasio" como lugar principal

### MT-places-05: Persistencia de datos
- **Criterios cubiertos:** RF-06
- **Precondiciones:** Tener 2+ lugares personalizados configurados
- **Pasos:**
  1. Cerrar completamente la app
  2. Reabrir app
  3. Ir a Settings → Driving → Mis lugares
- **Resultado esperado:** Todos los lugares personalizados siguen presentes

### MT-places-06: Migración desde versión anterior
- **Criterios cubiertos:** RF-07
- **Precondiciones:** Datos de versión anterior (sin customPlaces, solo Casa/Trabajo)
- **Pasos:**
  1. Actualizar app con nueva implementación
  2. Abrir Settings → Driving → Mis lugares
- **Resultado esperado:** Casa y Trabajo aparecen correctamente, sin errores

### MT-places-07: Editar nombre de Casa/Trabajo
- **Criterios cubiertos:** RF-03
- **Precondiciones:** Casa y Trabajo configurados
- **Pasos:**
  1. Tocar ícono Editar en "Casa"
  2. Cambiar nombre a "Casa Campo"
  3. Tocar "Guardar"
- **Resultado esperado:** Nombre actualizado, ID "home" se mantiene

---

## Dependencias y Decisiones Pendientes

### Técnicas (bloquean desarrollo)

| # | Duda | Tipo | Bloquea | Propuesta | Respuesta del propietario |
|---|------|------|---------|-----------|---------------------------|
| T-01 | ¿El widget Home/Work debe mostrar SOLO el lugar seleccionado, o mantener Casa/Trabajo como botones separados Y mostrar el seleccionado adicional? | Técnica | ✅ **RESUELTO** | **Decisión:** Widget muestra 1-4 lugares seleccionados con distribución en hasta 2 líneas:
- **1 lugar:** 1 línea, 100% ancho
- **2 lugares:** 1 línea, 50% cada uno
- **3 lugares:** 2 líneas (línea 1: 100%, línea 2: 50%+50%)
- **4 lugares:** 2 líneas (50%+50% en cada línea)

Usuario selecciona hasta 4 lugares en Settings → Mis Lugares → "Mostrar en widget". | **27/10/2026:** Aceptado por propietario. T-01 resuelto. |
| T-02 | ¿Los lugares personalizados deben tener icono personalizable o todos usan el mismo icono genérico? | Técnica | ✅ **RESUELTO** | **Decisión:** Iconos por categoría implementados desde el MVP:
- **Categorías iniciales:** Casa (`Icons.Filled.Home`), Trabajo (`Icons.Filled.Work`), Genérico (`Icons.Filled.Place`), Gimnasio (`Icons.Filled.FitnessCenter`), Escuela (`Icons.Filled.School`), Hospital (`Icons.Filled.LocalHospital`), Restaurante (`Icons.Filled.Restaurant`), Tienda (`Icons.Filled.ShoppingCart`)
- **Compatibilidad:** Lugares existentes "Casa" y "Trabajo" mantienen sus categorías fijas
- **Selección en UI:** Al crear/editar lugar personalizado, dropdown para elegir categoría
- **Extensible:** Se pueden agregar más categorías fácilmente
- **Default:** Categoría "Genérico" (`Icons.Filled.Place`) para nuevos lugares | **27/10/2026:** Aceptado por propietario. T-02 resuelto con iconos por categoría en MVP, incluyendo Casa y Trabajo como categorías base. |

### De Producto (no bloquean)

| # | Duda | Tipo | Bloquea | Propuesta | Respuesta del propietario |
|---|------|------|---------|-----------|---------------------------|
| P-01 | ¿Hay límite en cantidad de lugares personalizados? | Producto | ✅ **RESUELTO** | **Decisión:** Máximo **4 lugares personalizados**. Límite basado en:
- **Widget limitation:** Solo puede mostrar hasta 4 lugares
- **UX consideration:** Más de 4 lugares sería difícil de gestionar
- **Technical simplicity:** Validación sencilla en UI
- **Future expansion:** Si hay demanda, se puede aumentar el límite posteriormente | **27/10/2026:** Aceptado por propietario. Máximo 4 lugares personalizados. |
| P-02 | ¿Orden de lugares personalizados en la lista? | Producto | ✅ **RESUELTO** | **Decisión:** Orden de creación (más reciente al final) para MVP, con opciones futuras:
- **MVP:** Orden de creación (más antiguo primero, más reciente último)
- **Future 1:** Opción de orden alfabético
- **Future 2:** Opción de orden por uso frecuente
- **Future 3:** Reordenamiento manual drag-and-drop
- **UI:** Lista sencilla en Settings → Mis Lugares | **27/10/2026:** Aceptado por propietario. Orden de creación para MVP. |

---

## Módulos Afectados

| Módulo | Archivo | Cambios |
|--------|---------|---------|
| **Modelo de datos** | [`Places.kt`](app/src/main/java/com/openauto/dash/Places.kt) | Nueva estructura `SavedPlace` con ID, lista `customPlaces`, campo `selectedPlaceId` |
| **Storage** | [`Places.kt`](app/src/main/java/com/openauto/dash/Places.kt) (PlacesStore) | Métodos: `addCustomPlace`, `updatePlaceName`, `removeCustomPlace`, `setSelectedPlace`, migración legacy |
| **UI Settings** | [`HandsFreeSettings.kt`](app/src/main/java/com/openauto/dash/HandsFreeSettings.kt) | Nuevo `PlacesSheet` con lista de lugares personalizados, agregar, editar, eliminar, selector de widget |
| **Widget** | [`HomeWorkTile.kt`](app/src/main/java/com/openauto/dash/HomeWorkTile.kt) | Mostrar lugar seleccionado en lugar de siempre Casa/Trabajo |
| **Strings EN** | [`strings_places.xml`](app/src/main/res/values/strings_places.xml) | Nuevos strings para UI de lugares personalizados |
| **Strings ES** | [`strings_places.xml`](app/src/main/res/values-es/strings_places.xml) | Traducciones de nuevos strings |

---

## Plan de Implementación (Incrementos)

### Incremento 1: Modelo de datos
- [ ] Crear `SavedPlace` data class con id, name, lat, lng, isBuiltIn
- [ ] Actualizar `Places` data class con `customPlaces: List<SavedPlace>`, `selectedPlaceId: String?`
- [ ] Agregar método `getSelectedPlace()` en `Places`
- [ ] Métodos: `addCustomPlace`, `updateCustomPlaceName`, `removeCustomPlace`, `setSelectedPlace`

### Incremento 2: Storage (PlacesStore)
- [ ] Actualizar `encode()` para serializar `customPlaces` y `selectedPlaceId`
- [ ] Actualizar `decode()` para deserializar nuevos campos
- [ ] Implementar `migrateLegacyData()` para backward compatibility
- [ ] Métodos públicos: `addCustomPlace`, `updatePlaceName`, `removeCustomPlace`, `setSelectedPlace`

### Incremento 3: UI Settings
- [ ] Modificar `PlacesSheet` para mostrar sección de lugares personalizados
- [ ] Agregar botón "Agregar lugar personalizado" con dialog
- [ ] Implementar lista de lugares personalizados con editar/eliminar
- [ ] Agregar dropdown selector "Mostrar en widget"
- [ ] Permitir editar nombre de Casa/Trabajo también

### Incremento 4: Widget
- [ ] Modificar `HomeWorkTile` para usar `places.getSelectedPlace()`
- [ ] Mostrar lugar seleccionado en widget
- [ ] Mantener backward compatibility si no hay selección

### Incremento 5: Strings multi-idioma
- [ ] Agregar strings en inglés (`values/strings_places.xml`)
- [ ] Agregar strings en español (`values-es/strings_places.xml`)

### Incremento 6: Validación
- [ ] Ejecutar tests unitarios existentes: `./gradlew :app:testGithubDebugUnitTest`
- [ ] Ejecutar lint: `./gradlew :app:lintGithubDebug`
- [ ] Build de prueba: `./gradlew :app:assembleGithubDebug`
- [ ] Pruebas manuales MT-places-01 a MT-places-07

---

## Riesgos

| Riesgo | Impacto | Probabilidad | Mitigación |
|--------|---------|--------------|------------|
| Migración de datos legacy falla | Alto | Baja | Mantener backward compatibility, defaults seguros, testing exhaustivo |
| UI se vuelve muy larga con muchos lugares | Medio | Media | Considerar scroll o paginación si >10 lugares |
| Widget pierde funcionalidad actual | Alto | Baja | Testing exhaustivo de backward compatibility, mantener lógica existente |
| Confusión de usuario con selector de widget | Medio | Media | UX clara, label descriptivo, valor por defecto razonable (Casa) |

---

## Estado de Aprobación

- [ ] **Revisión de spec:** Pendiente
- [x] **Dudas resueltas:** 4/4 respondidas (T-01 ✅, T-02 ✅, P-01 ✅, P-02 ✅)
- [ ] **Autorización para implementar:** No autorizada aún
- [x] **Ticket GitHub creado:** ✅ [#1](https://github.com/ravalosv/MyDashWheel/issues/1)

**Próximos pasos:**
1. Propietario revisa spec y responde dudas T-01, T-02, P-01, P-02
2. Una vez todas las dudas técnicas respondidas, autorizar implementación
3. Implementar siguiendo incrementos 1-6
4. Ejecutar validación y reporte de resultados

---

## Notas

- **No implementar hasta que todas las dudas técnicas tengan respuesta y haya autorización explícita**
- Mantener commits locales hasta autorización de push
- Seguir quality gates del harness (lint, tests, build)
