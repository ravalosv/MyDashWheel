# Implementar issue

Issue: $ARGUMENTS

Antes de entregar el desarrollo como terminado, ejecutar automáticamente y verificar la regresión completa configurada (lint, tests unitarios, build) sobre la versión final, según `docs/harness/quality-gates.md`. No basta con pruebas focalizadas. Si una comprobación requerida falla, se omite o está bloqueada, reportar BLOCKED/incompleto y no aprobar el DoD ni cerrar como Done/Listo. Registrar evidencia real de comandos, fecha, versión/estado del árbol, entorno, cantidades y códigos de salida.

Aplica el procedimiento `docs/harness/workflows/implement-story.md`. Trabaja solo con issues de `ravalosv/MyDashWheel`; no exijas `ready-for-agent` ni apliques triage del upstream. Antes de editar, revisa dependencias y decisiones pendientes, confirma autorización explícita del propietario y presenta plan e impacto. Implementa el cambio mínimo con tests, límites modulares y contratos. Ejecuta solo comandos disponibles y autorizados. Reporta exactamente qué se ejecutó, archivos modificados, riesgos y criterios cubiertos. No hagas push, merge ni actualización de issue sin autorización.
