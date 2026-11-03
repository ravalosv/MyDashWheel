# Implementar issue

Issue: $ARGUMENTS

Aplicar la validación por riesgo de `docs/harness/quality-gates.md`: checks focalizados durante desarrollo y una regresión GitHub por lote aceptado antes de APK para uso habitual o cierre. Play solo por solicitud explícita. Distinguir listo para revisión de validado para uso habitual; reutilizar evidencia válida y no ejecutar Gradle para documentación. Si un check requerido falla, se omite o está bloqueado, reportar BLOCKED/incompleto para esa etapa.

Aplica `docs/harness/workflows/implement-story.md`. Trabaja solo con issues de `ravalosv/MyDashWheel`, sin etiquetas upstream. Antes de editar, revisa dependencias, resuelve decisiones bloqueantes y confirma autorización explícita. Resume alcance, impacto y pruebas; planes extensos y revisores especializados solo si el riesgo lo justifica. Implementa el cambio mínimo con tests y límites modulares. Reporta brevemente versión, comandos/resultados reales, omisiones y riesgos. No hagas commit, push, merge ni actualización de issue sin autorización.
