# HU-11: resultados de la prueba de carga

**Historia:** sostener un pico de consulta sin afectar el registro (ISW-45).
**Fecha de la prueba:** 23 de noviembre de 2026.
**Resultado:** se cumplen los 4 criterios de aceptación.

## Cómo se hizo

- Todo BioHub corriendo con `docker compose`, con las **2 réplicas** de la api-pública detrás de Nginx y Redis como caché.
- Base de datos con **10.011 hallazgos**: los 11 de los datos de prueba más los 10.000 de `datos/datos-10000.sql`.
- k6 2.3.0 en un contenedor conectado a la red de BioHub, entrando por Nginx como un usuario real.
- Script: [`hu11-pico-consulta.js`](../../k6/hu11-pico-consulta.js). Tres escenarios al mismo tiempo durante 2 minutos:

| Escenario | Qué simula |
|---|---|
| `busqueda` | Sube a **1000 usuarios simultáneos** buscando especies en la consulta pública |
| `ficha_noticia` | Una ficha pasa de **1 a 70 visitas por segundo** (70 veces el tráfico normal) |
| `registro` | Un investigador registra **un hallazgo cada 5 segundos** durante todo el pico |

## Resultados por criterio

1. **El tráfico a una ficha se multiplica por más de 60 y la consulta responde dentro de los 2 s.** Con 70 veces el tráfico normal, el 95 % de las respuestas tardó menos de 1,8 ms y la más lenta, 14 ms. Se cumple.
2. **Durante el pico, el registro de hallazgos mantiene sus 5 s.** El registro más lento tardó 32 ms y el promedio fue 19 ms. Todos quedaron guardados. Se cumple.
3. **Con 1000 consultas simultáneas no se alteran los tiempos.** Con 1000 usuarios buscando, el 95 % de las búsquedas tardó menos de 2,1 ms y la más lenta, 54 ms. Se cumple.
4. **Con 10.000 registros las consultas responden en menos de 3 s.** Con 10.011 registros, el 99 % tardó menos de 4,1 ms en la búsqueda y 3,4 ms en la ficha. Se cumple.

**Respuestas correctas:** 100 % (87.662 de 87.662 comprobaciones). **Peticiones fallidas:** 0 de 87.664.
**Ritmo:** unas 730 peticiones por segundo en promedio.

## Notas técnicas comprobadas

- **Las 2 réplicas reparten la carga.** Nginx alterna las respuestas entre ellas; se ve en la cabecera `X-BioHub-Replica`.
- **Redis responde lo repetido.** Por eso la búsqueda y la ficha tardan milisegundos aunque haya 10.000 registros.
- **Si una réplica de la api-pública se cae, el resto sigue vivo (ASR-3).** Se detuvo a la fuerza una réplica: Docker la reinició sola, la consulta pública siguió respondiendo 200 con la otra réplica y el login de la api-interna siguió respondiendo 200.

## Límites de la prueba

- k6 y BioHub corrieron en el mismo computador (MacBook Air con chip Apple). En un servidor real los números cambian, pero la prueba muestra que la arquitectura aguanta el pico con mucho margen.
- Las búsquedas usan 8 términos que se repiten, como pasa cuando una especie se vuelve noticia. Por eso Redis atiende casi todo.

## Evidencia

- [`salida.txt`](salida.txt): la salida completa de k6.
- [`resumen.json`](resumen.json): todos los números en JSON.

## Cómo repetirla

Ver la sección **HU-11** de [`pruebas/README.md`](../../README.md#hu-11-pico-de-consulta-pública).
