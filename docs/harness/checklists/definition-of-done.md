# Definition of Done — verificación humana

Aplicar [quality-gates.md](../quality-gates.md); no exigir checks ajenos al alcance.

## Listo para revisión
- [ ] Issue de `ravalosv/MyDashWheel`, autorización y decisiones bloqueantes resueltas (código de la app).
- [ ] Criterios cubiertos por pruebas apropiadas y casos negativos relevantes.
- [ ] Checks focalizados por riesgo satisfactorios sobre la versión entregada.
- [ ] Casos manuales necesarios de UI, hardware o permisos definidos y resultados reales declarados.
- [ ] Límites modulares, Android 9+, privacidad y documentación revisados según impacto.
- [ ] Play conservado; checks solo si se solicitaron explícitamente.
- [ ] Reporte breve: alcance, versión, comandos/resultados, checks no ejecutados y riesgos.
- [ ] Ningún check requerido fallido, omitido o bloqueado; en caso contrario BLOCKED/incompleto.

## Lote aceptado / uso habitual / cierre
- [ ] Aceptación humana obtenida.
- [ ] Regresión GitHub del lote final satisfactoria, incluidos otros módulos afectados; evidencia local o CI válida sin duplicarla.
- [ ] Casos manuales requeridos ejecutados con resultado Pass/Fail/Blocked y evidencia.
- [ ] Cierre, commit, merge o push solo con la autorización correspondiente.
