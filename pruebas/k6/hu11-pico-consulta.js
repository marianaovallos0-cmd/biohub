// HU-11: sostener un pico de consulta sin afectar el registro.
// Corren tres escenarios a la vez:
//   busqueda      -> hasta 1000 usuarios simultáneos buscando especies (criterio 3)
//   ficha_noticia -> una ficha pasa de 1 a 70 visitas por segundo, más de 60 veces (criterio 1)
//   registro      -> un investigador registra un hallazgo cada 5 segundos mientras tanto (criterio 2)
// La base debe tener los 10.000 registros de datos-10000.sql (criterio 4).
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE || 'http://host.docker.internal:8080';

const ANFIBIO = '30000000-0000-0000-0000-000000000001';
const TEMPERATURA_AGUA = '31000000-0000-0000-0000-000000000001';
const BUSQUEDAS = ['oophaga', 'atelopus', 'agalychnis', 'ara macao', 'ramphastos', 'vultur', 'rupicola', 'crax'];

export const options = {
  scenarios: {
    busqueda: {
      executor: 'ramping-vus', exec: 'buscar',
      stages: [
        { duration: '30s', target: 1000 },
        { duration: '1m', target: 1000 },
        { duration: '15s', target: 0 },
      ],
    },
    ficha_noticia: {
      executor: 'ramping-arrival-rate', exec: 'verFicha',
      startRate: 1, timeUnit: '1s', preAllocatedVUs: 100, maxVUs: 300,
      stages: [
        { duration: '30s', target: 1 },  // tráfico normal: 1 visita por segundo
        { duration: '10s', target: 70 }, // la especie se vuelve noticia
        { duration: '1m', target: 70 },  // 70 veces el tráfico normal
        { duration: '5s', target: 0 },
      ],
    },
    registro: {
      executor: 'constant-arrival-rate', exec: 'registrar',
      rate: 1, timeUnit: '5s', duration: '2m', preAllocatedVUs: 2,
    },
  },
  thresholds: {
    // Criterios 1 y 3: la consulta pública responde en menos de 2 segundos (p95)
    // Criterio 4: con 10.000 registros, casi todas en menos de 3 segundos (p99)
    'http_req_duration{scenario:busqueda}': ['p(95)<2000', 'p(99)<3000'],
    'http_req_duration{scenario:ficha_noticia}': ['p(95)<2000', 'p(99)<3000'],
    // Criterio 2: cada registro tarda menos de 5 segundos
    'http_req_duration{scenario:registro}': ['max<5000'],
    // Respuestas correctas
    'checks{scenario:busqueda}': ['rate>0.99'],
    'checks{scenario:ficha_noticia}': ['rate>0.99'],
    'checks{scenario:registro}': ['rate==1'],
  },
};

// Se corre una vez antes de la prueba: inicia sesión y elige la ficha "que se vuelve noticia"
export function setup() {
  const login = http.post(`${BASE}/interno/auth/login`,
    JSON.stringify({ correo: 'investigador1@biohub.test', contrasena: 'biohub123' }),
    { headers: { 'Content-Type': 'application/json' } });
  const token = login.json('token');
  const busqueda = http.get(`${BASE}/publico/busqueda?q=oophaga`);
  const fichaId = busqueda.json('resultados.0.hallazgoId');
  if (!token || !fichaId) {
    throw new Error('No se pudo preparar la prueba: revisa que el sistema esté arriba y con datos.');
  }
  return { token, fichaId };
}

export function buscar() {
  const texto = BUSQUEDAS[Math.floor(Math.random() * BUSQUEDAS.length)];
  const r = http.get(`${BASE}/publico/busqueda?q=${encodeURIComponent(texto)}`);
  check(r, { 'la búsqueda responde 200': (x) => x.status === 200 });
  sleep(1);
}

export function verFicha(datos) {
  const r = http.get(`${BASE}/publico/fichas/${datos.fichaId}`);
  check(r, { 'la ficha responde 200': (x) => x.status === 200 });
}

export function registrar(datos) {
  const hallazgo = {
    expedicion: 'Prueba HU-11',
    ficha: {
      tipoEspecieId: ANFIBIO, reino: 'Animalia', filo: 'Chordata', clase: 'Amphibia', orden: 'Anura',
      familia: 'Dendrobatidae', genero: 'Oophaga', especie: 'histrionica',
      valores: { [TEMPERATURA_AGUA]: '24.5' },
    },
    ubicacion: { latitud: 5.7, longitud: -76.6, region: 'Pacífica', departamento: 'Chocó', especieProtegida: true },
  };
  const r = http.post(`${BASE}/interno/hallazgos`,
    { datos: http.file(JSON.stringify(hallazgo), 'datos.json', 'application/json') },
    { headers: { Authorization: `Bearer ${datos.token}` } });
  check(r, { 'el hallazgo se registra (201)': (x) => x.status === 201 });
}
