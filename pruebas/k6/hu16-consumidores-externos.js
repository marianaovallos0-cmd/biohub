// HU-16: consumir los datos desde un sistema externo.
// Corren dos escenarios a la vez:
//   consumidores -> 500 consumidores externos, cada uno con su clave, piden datos cada 2 segundos.
//                   Así ninguno pasa su límite de 60 por minuto (eso lo prueba HU-17).
//                   La respuesta debe llegar en menos de 2 s y sin coordenadas (criterios 1 y 4).
//   restringido  -> con clave válida se pide una ruta no habilitada (403)
//                   y con clave falsa se piden los datos (401) (criterio 2).
// La base debe tener los 500 consumidores de datos/consumidores-500.sql.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE || 'http://host.docker.internal:8080';

export const options = {
  scenarios: {
    consumidores: {
      executor: 'ramping-vus', exec: 'consultar',
      stages: [
        { duration: '30s', target: 500 },
        { duration: '1m', target: 500 },
        { duration: '10s', target: 0 },
      ],
    },
    restringido: {
      executor: 'constant-arrival-rate', exec: 'pedirRestringido',
      rate: 1, timeUnit: '5s', duration: '1m40s', preAllocatedVUs: 2,
    },
  },
  thresholds: {
    'http_req_duration{scenario:consumidores}': ['p(95)<2000'],
    'checks{scenario:consumidores}': ['rate>0.99'],
    'checks{scenario:restringido}': ['rate==1'],
  },
};

// Cada usuario virtual de k6 es un consumidor distinto: el 1 usa carga-hu16-001, el 2 carga-hu16-002...
export function consultar() {
  const clave = `carga-hu16-${String(__VU).padStart(3, '0')}`;
  const r = http.get(`${BASE}/externo/hallazgos?tamano=20`, { headers: { 'X-API-Key': clave } });
  check(r, {
    'el consumidor recibe los datos (200)': (x) => x.status === 200,
    'la respuesta no trae coordenadas': (x) => !String(x.body).includes('latitud'),
  });
  sleep(2);
}

export function pedirRestringido() {
  const noHabilitada = http.get(`${BASE}/externo/ubicaciones`, { headers: { 'X-API-Key': 'dev-key-universidad' } });
  const claveFalsa = http.get(`${BASE}/externo/hallazgos`, { headers: { 'X-API-Key': 'clave-que-no-existe' } });
  check(noHabilitada, { 'una ruta no habilitada se rechaza (403)': (x) => x.status === 403 });
  check(claveFalsa, { 'una clave falsa se rechaza (401)': (x) => x.status === 401 });
}
