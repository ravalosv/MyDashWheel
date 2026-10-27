# Workflow: preparar issue

1. Leer instrucciones del repositorio (`AGENTS.md`) y ADR aplicables.
2. Localizar issue por número, dependencias y estado.
3. Crear matriz requisito → criterio → prueba.
4. Definir casos de prueba manuales concretos. Cada caso lleva ID estable `MT-<issue>-NN`, criterios de aceptación cubiertos, precondiciones/datos ficticios, pasos numerados y resultado esperado observable.
5. Incluir esos casos en una sección **Pruebas manuales** de la descripción del issue.
6. Registrar cada duda como pregunta concreta con contexto, sugerencia y espacio para respuesta.
   - Clasificar como `Técnica` o `Producto`.
   - Indicar si bloquea desarrollo.
   - Incluir propuesta con valores, rangos, defaults y límites cuando aplique.
   - Dejar `Respuesta del propietario: Pendiente`.
7. Una pregunta `Pendiente` bloquea iniciar la construcción.
8. Si no hay dudas, escribir explícitamente: **"No hay dudas ni pendientes para este issue; está listo para implementar."**
9. Entregar plan acotado, archivos/módulos afectados y riesgos.

**Salida:** plan en comentario del issue o en `docs/harness/reports/<issue>-plan.md`. No implementar durante la preparación.
