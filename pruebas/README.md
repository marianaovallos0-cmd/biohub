# Pruebas de carga

Comprueban los criterios de las HU que piden tiempos o límites. Se corren con [k6](https://k6.io) en un contenedor de Docker, entrando por Nginx como lo haría un usuario real.

| Carpeta | Qué hay |
|---|---|
| `k6/` | Los scripts de cada prueba |
| `datos/` | Los datos que hay que cargar antes de algunas pruebas |
| `resultados/` | La salida de cada prueba, en una carpeta por HU |

| HU | Script | Datos antes | Dura |
|---|---|---|---|
| HU-11 | `hu11-pico-consulta.js` | `datos-10000.sql` | 2 min |
| HU-16 | `hu16-consumidores-externos.js` | `consumidores-500.sql` | 2 min |
| HU-17 | `hu17-limite.js` | Ninguno | 20 s |
| HU-19 | `hu19-analitica-concurrente.js` | `datos-10000.sql` (opcional) | 2 min |

## Cómo correr una prueba

Todo se corre desde la carpeta del proyecto, con BioHub levantado (`docker compose up -d`).

1. Si la prueba necesita datos, cárgalos. Se pueden correr varias veces, porque cada script borra su carga anterior:
   ```
   docker compose exec -T db psql -U postgres -d biohub < pruebas/datos/datos-10000.sql
   ```
2. Crea la carpeta de resultados de la HU, por ejemplo `pruebas/resultados/hu16`.
3. Corre el script. Este es el de HU-16; para otra HU cambia el nombre del script y la carpeta de resultados:
   ```
   docker run --rm --network biohub_default -v "%cd%/pruebas:/pruebas" -e BASE=http://nginx grafana/k6:2.3.0 run --summary-export=/pruebas/resultados/hu16/resumen.json /pruebas/k6/hu16-consumidores-externos.js
   ```
   En Mac o Linux cambia `"%cd%/pruebas"` por `"$PWD/pruebas"`.
4. Si al final todos los `THRESHOLDS` salen con ✓, se cumplen los criterios. Guarda la salida de la consola en `salida.txt`, en la misma carpeta de resultados.

## HU-11: pico de consulta pública

Una ficha pasa de 1 a 70 visitas por segundo y 1000 usuarios buscan al mismo tiempo, mientras un investigador sigue registrando hallazgos. Los resultados ya están en [`resultados/hu11/HU-11-resultados.md`](resultados/hu11/HU-11-resultados.md).

## HU-16: 500 consumidores externos

500 consumidores, cada uno con su clave, piden datos al tiempo. La respuesta debe llegar en menos de 2 s y sin coordenadas. Antes de la carga se comprueba que una ruta no habilitada responde 403 y una clave falsa responde 401.

## HU-17: límite de peticiones

La ONG (60 por minuto) hace 100 peticiones seguidas y recibe 429 en las que pasan su límite. Mientras tanto, la universidad (120 por minuto) sigue recibiendo 200.

## HU-19: analítica sin frenar el registro

El director pide 12 reportes de una vez y cada pedido responde de inmediato. Mientras la analítica los calcula, 300 usuarios buscan y un investigador registra hallazgos. Al final se comprueba que los 12 reportes se generaron.
