# Workflow: implementar issue

1. Confirmar issue de `ravalosv/MyDashWheel`, autorización explícita y decisiones bloqueantes resueltas. No exigir etiquetas del upstream.
2. Leer contexto y resumir alcance, impacto, riesgo y checks según [quality-gates.md](../quality-gates.md). Plan extenso o revisión especializada solo si se justifican.
3. Implementar el cambio mínimo completo con pruebas de reglas críticas, regresiones corregidas y casos negativos.
4. Ejecutar compilación GitHub y tests focalizados apropiados; ampliar a módulos/consumidores afectados según riesgo. Documentación no requiere Gradle; scripts del harness requieren validación propia.
5. Revisar UI/orientaciones, hardware, permisos, datos y límites modulares afectados. Mantener Play; ejecutar sus checks solo por solicitud explícita.
6. Entregar **listo para revisión** con reporte breve según [implementation-report.md](../templates/implementation-report.md). Registrar versión, comandos/resultados reales y checks no ejecutados con motivo.
7. Tras aceptar el lote, ejecutar una regresión GitHub de la versión final antes de APK para uso habitual o cierre de sus issues. Reutilizar evidencia válida local o CI de la misma versión, entorno y alcance; no repetir por cada ajuste o commit.
8. Un check requerido fallido, omitido o bloqueado impide aprobar la etapa: corregir o reportar BLOCKED/incompleto. Un check no requerido se declara como tal, no como PASS.
9. Registrar pruebas manuales necesarias con resultados reales y aceptación humana. Actualizar documentación/ADR si corresponde.
10. No hacer commit, push, merge ni cerrar el issue sin la autorización correspondiente.
