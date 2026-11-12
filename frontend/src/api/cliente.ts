const CLAVE_TOKEN = 'biohub_token';

export function guardarToken(token: string) {
  sessionStorage.setItem(CLAVE_TOKEN, token);
}

export function borrarToken() {
  sessionStorage.removeItem(CLAVE_TOKEN);
}

// Todas las llamadas a la API pasan por aquí.
// Rutas relativas (/publico/..., /interno/...): Vite o Nginx las mandan a la API correcta.
export async function pedir<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  const token = sessionStorage.getItem(CLAVE_TOKEN);
  const respuesta = await fetch(ruta, {
    ...opciones,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...opciones.headers,
    },
  });
  if (!respuesta.ok) {
    throw new Error(`Error ${respuesta.status} en ${ruta}`);
  }
  if (respuesta.status === 204) {
    return undefined as T;
  }
  return respuesta.json() as Promise<T>;
}

export function tieneToken() {
  return sessionStorage.getItem(CLAVE_TOKEN) !== null;
}
