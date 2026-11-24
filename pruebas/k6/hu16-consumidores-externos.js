// HU-16: consumir los datos desde un sistema externo.
// Antes de la carga (setup) se comprueba el criterio 2: con clave válida una ruta no habilitada
// responde 403, y con clave falsa los datos responden 401.
// Después, 500 consumidores externos, cada uno con su clave, piden datos cada 2 segundos.
// Así ninguno pasa su límite de 60 por minuto (eso lo prueba HU-17).
// La respuesta debe llegar en menos de 2 s y sin coordenadas (criterios 1 y 4).
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
  },
  thresholds: {
    'http_req_duration{scenario:consumidores}': ['p(95)<2000'],
    'checks{scenario:consumidores}': ['rate>0.99'],
    'checks{name:restringido}': ['rate==1'],
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

// setup no usa usuarios virtuales, así los consumidores quedan numerados del 1 al 500
export function setup() {
  const noHabilitada = http.get(`${BASE}/externo/ubicaciones`, { headers: { 'X-API-Key': 'dev-key-universidad' } });
  const claveFalsa = http.get(`${BASE}/externo/hallazgos`, { headers: { 'X-API-Key': 'clave-que-no-existe' } });
  check(noHabilitada, { 'una ruta no habilitada se rechaza (403)': (x) => x.status === 403 }, { name: 'restringido' });
  check(claveFalsa, { 'una clave falsa se rechaza (401)': (x) => x.status === 401 }, { name: 'restringido' });
}
