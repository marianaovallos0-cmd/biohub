# HU-11: resultados de la prueba de carga

**Historia:** sostener un pico de consulta sin afectar el registro (ISW-45).
**Fecha de la prueba:** 23 de noviembre de 2026.
**Resultado:** ✅ se cumplen los 4 criterios de aceptación.

## Cómo se hizo

- Todo BioHub corriendo con `docker compose`, con las **2 réplicas** de la api-pública detrás de Nginx y Redis como caché.
- Base de datos con **10.011 hallazgos**: los 11 de los datos de prueba más los 10.000 de `datos-10000.sql`.
- k6 2.3.0 en un contenedor conectado a la red de BioHub, entrando por Nginx como un usuario real.
- Script: [`hu11-pico-consulta.js`](hu11-pico-consulta.js). Tres escenarios al mismo tiempo durante 2 minutos:

| Escenario | Qué simula |
|---|---|
| `busqueda` | Sube a **1000 usuarios simultáneos** buscando especies en la consulta pública |
| `ficha_noticia` | Una ficha pasa de **1 a 70 visitas por segundo** (70 veces el tráfico normal) |
| `registro` | Un investigador registra **un hallazgo cada 5 segundos** durante todo el pico |

## Resultados por criterio

| # | Criterio | Meta | Medido | ¿Cumple? |
|---|---|---|---|---|
| 1 | El tráfico a una ficha se multiplica por más de 60 y la consulta responde dentro de los 2 s | p95 < 2 s con más de 60 veces el tráfico | ×70 el tráfico, **p95 = 1,8 ms**, máximo 14 ms | ✅ |
| 2 | Durante el pico, el registro de hallazgos mantiene sus 5 s | Ningún registro tarda más de 5 s | **máximo = 32 ms**, promedio 19 ms, todos respondieron 201 | ✅ |
| 3 | Con 1000 consultas simultáneas no se alteran los tiempos | p95 < 2 s con 1000 usuarios | 1000 usuarios, **p95 = 2,1 ms**, máximo 54 ms | ✅ |
| 4 | Con 10.000 registros las consultas responden en menos de 3 s | p99 < 3 s | 10.011 registros, **p99 = 4,1 ms** (búsqueda) y 3,4 ms (ficha) | ✅ |

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

- [`resultados/hu11-salida.txt`](resultados/hu11-salida.txt): la salida completa de k6.
- [`resultados/hu11-resumen.json`](resultados/hu11-resumen.json): todos los números en JSON.

## Cómo repetirla

Ver la sección **Prueba de carga (HU-11)** del [README](../../README.md#prueba-de-carga-hu-11).
