// HU-17: límite de peticiones por consumidor externo.
//   ong         -> la ONG (60 por minuto) hace 100 peticiones seguidas: las que pasan el límite reciben 429.
//   universidad -> al mismo tiempo, la universidad (120 por minuto) pide 1 por segundo y no la frenan.
// Así se ve que un consumidor que se pasa del límite no afecta a los demás.
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://host.docker.internal:8080';

const rechazosOng = new Counter('rechazos_ong');
const rechazosUniversidad = new Counter('rechazos_universidad');

export const options = {
  scenarios: {
    ong: { executor: 'shared-iterations', vus: 5, iterations: 100, exec: 'ong' },
    universidad: {
      executor: 'constant-arrival-rate', rate: 1, timeUnit: '1s',
      duration: '20s', preAllocatedVUs: 5, exec: 'universidad',
    },
  },
  thresholds: {
    rechazos_ong: ['count>0'],
    rechazos_universidad: ['count==0'],
    'checks{scenario:universidad}': ['rate==1'],
  },
};

function pedir(clave) {
  return http.get(`${BASE}/externo/hallazgos?tamano=10`, { headers: { 'X-API-Key': clave } });
}

export function ong() {
  rechazosOng.add(pedir('dev-key-ong').status === 429 ? 1 : 0);
}

export function universidad() {
  const r = pedir('dev-key-universidad');
  rechazosUniversidad.add(r.status === 429 ? 1 : 0);
  check(r, { 'la universidad recibe 200': (x) => x.status === 200 });
}