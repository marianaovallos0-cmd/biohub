const CLAVE_TOKEN = 'biohub_token';

export function guardarToken(token: string) {
  sessionStorage.setItem(CLAVE_TOKEN, token);
}

export function borrarToken() {
  sessionStorage.removeItem(CLAVE_TOKEN);
}

// Error de la API con lo que respondió el backend (ProblemDetail: detail y, si hay, la lista de errores)
export class ErrorApi extends Error {
  readonly estado: number;
  readonly detalle?: string;
  readonly errores: string[];

  constructor(estado: number, ruta: string, cuerpo?: { detail?: string; errores?: string[] }) {
    super(`Error ${estado} en ${ruta}`);
    this.estado = estado;
    this.detalle = cuerpo?.detail;
    this.errores = cuerpo?.errores ?? [];
  }
}

// Todas las llamadas a la API pasan por aquí.
// Rutas relativas (/publico/..., /interno/...): Vite o Nginx las mandan a la API correcta.
export async function pedir<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  const token = sessionStorage.getItem(CLAVE_TOKEN);
  // Con FormData (archivos) el navegador pone el Content-Type con su "boundary"; no hay que ponerlo a mano
  const esFormulario = opciones.body instanceof FormData;
  const respuesta = await fetch(ruta, {
    ...opciones,
    headers: {
      ...(esFormulario ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...opciones.headers,
    },
  });
  if (!respuesta.ok) {
    const cuerpo = await respuesta.json().catch(() => undefined);
    throw new ErrorApi(respuesta.status, ruta, cuerpo);
  }
  if (respuesta.status === 204) {
    return undefined as T;
  }
  return respuesta.json() as Promise<T>;
}

// Para archivos protegidos (evidencias): el navegador no manda el token en un <a href>, por eso se pide con fetch
export async function pedirArchivo(ruta: string): Promise<Blob> {
  const token = sessionStorage.getItem(CLAVE_TOKEN);
  const respuesta = await fetch(ruta, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  if (!respuesta.ok) {
    const cuerpo = await respuesta.json().catch(() => undefined);
    throw new ErrorApi(respuesta.status, ruta, cuerpo);
  }
  return respuesta.blob();
}

export function tieneToken() {
  return sessionStorage.getItem(CLAVE_TOKEN) !== null;
}
