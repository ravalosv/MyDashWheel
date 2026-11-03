# Workflow: preparar issue

1. Leer instrucciones del repositorio (`AGENTS.md`) y ADR aplicables.
2. Localizar el issue por número exclusivamente en `ravalosv/MyDashWheel` y revisar dependencias y decisiones. No filtrar ni bloquear por etiquetas/estados de triage del upstream.
3. Resumir criterios, riesgo y checks según [quality-gates.md](../quality-gates.md). Matriz y plan extenso solo cuando aporten valor por complejidad.
4. Definir solo casos manuales necesarios para UI, hardware, permisos o comportamiento no cubierto automáticamente. Usar ID `MT-<issue>-NN`, criterio, precondiciones, pasos y resultado observable.
5. Incluir los casos necesarios en el issue o plan; no duplicar tests automatizados sin motivo.
6. Registrar cada duda como pregunta concreta con contexto, sugerencia y espacio para respuesta.
   - Clasificar como `Técnica` o `Producto`.
   - Indicar si bloquea desarrollo.
   - Incluir propuesta con valores, rangos, defaults y límites cuando aplique.
   - Dejar `Respuesta del propietario: Pendiente`.
7. Una pregunta bloqueante con respuesta `Pendiente` impide iniciar la construcción; documentar el impacto de las no bloqueantes.
8. Si no hay bloqueos, declarar: **"Listo para implementar con autorización"**, indicando las dudas no bloqueantes si existen.
9. Entregar plan acotado, archivos/módulos afectados y riesgos.

**Salida:** plan breve en el issue o conversación; archivo solo si se solicita o es necesario por complejidad. No implementar durante la preparación.
