// HU-19: ejecutar la analítica sin frenar el registro diario (ASR-6).
// Mientras la analítica calcula reportes en su propio contenedor, corren:
//   busqueda -> 300 usuarios buscando en la consulta pública (criterio 2: menos de 2 s)
//   registro -> un investigador registra un hallazgo cada 5 segundos (criterio 1: menos de 5 s)
// En setup el director pide 12 reportes de una vez: cada pedido debe responder de inmediato,
// sin esperar el cálculo (criterio 3). La analítica los calcula en segundo plano en el siguiente minuto.
//
// Para más carga, conviene tener los 10.000 registros de datos/datos-10000.sql.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { buscar, registrar } from './hu11-pico-consulta.js';

const BASE = __ENV.BASE || 'http://host.docker.internal:8080';
const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  scenarios: {
    busqueda: {
      executor: 'ramping-vus', exec: 'buscar',
      stages: [
        { duration: '20s', target: 300 },
        { duration: '1m40s', target: 300 },
        { duration: '10s', target: 0 },
      ],
    },
    registro: {
      executor: 'constant-arrival-rate', exec: 'registrar',
      rate: 1, timeUnit: '5s', duration: '2m', preAllocatedVUs: 2,
    },
  },
  thresholds: {
    'http_req_duration{scenario:busqueda}': ['p(95)<2000'],
    'http_req_duration{scenario:registro}': ['max<5000'],
    'checks{scenario:busqueda}': ['rate>0.99'],
    'checks{scenario:registro}': ['rate==1'],
    // Criterio 3: pedir un reporte no bloquea al director
    'http_req_duration{name:pedir_reporte}': ['max<1000'],
    // Si la analítica no genera los reportes, la prueba falla
    'checks{name:reportes_generados}': ['rate==1'],
  },
};

function iniciarSesion(correo) {
  const r = http.post(`${BASE}/interno/auth/login`,
    JSON.stringify({ correo, contrasena: 'biohub123' }), { headers: JSON_HEADERS });
  return r.json('token');
}

// Los 12 meses terminados antes del actual, en formato AAAA-MM
function ultimosMeses(cuantos) {
  const meses = [];
  const hoy = new Date();
  for (let i = 1; i <= cuantos; i++) {
    const d = new Date(hoy.getFullYear(), hoy.getMonth() - i, 1);
    meses.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }
  return meses;
}

export function setup() {
  const token = iniciarSesion('investigador1@biohub.test');
  const director = iniciarSesion('director@biohub.test');
  const busqueda = http.get(`${BASE}/publico/busqueda?q=oophaga`);
  const fichaId = busqueda.json('resultados.0.hallazgoId');
  if (!token || !director || !fichaId) {
    throw new Error('No se pudo preparar la prueba: revisa que el sistema esté arriba y con datos.');
  }

  for (const mes of ultimosMeses(12)) {
    const r = http.post(`${BASE}/interno/reportes`, JSON.stringify({ mes }), {
      headers: { ...JSON_HEADERS, Authorization: `Bearer ${director}` },
      tags: { name: 'pedir_reporte' },
    });
    check(r, { 'el reporte queda pedido sin esperar el cálculo': (x) => x.status >= 200 && x.status < 300 });
  }
  return { token, fichaId, director };
}

export { buscar, registrar };

// Al final se comprueba que la analítica sí calculó los reportes en segundo plano
export function teardown(datos) {
  sleep(5);
  const r = http.get(`${BASE}/interno/reportes`, { headers: { Authorization: `Bearer ${datos.director}` } });
  const reportes = r.json() || [];
  const generados = reportes.filter((x) => x.generado).length;
  console.log(`Reportes generados por la analítica: ${generados} de ${reportes.length}`);
  check(r, { 'la analítica generó los reportes': () => generados >= 12 }, { name: 'reportes_generados' });
}
