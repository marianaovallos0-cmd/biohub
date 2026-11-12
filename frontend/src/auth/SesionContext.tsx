import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { borrarToken, guardarToken, pedir, tieneToken } from '@/api/cliente';

export type Rol = 'INVESTIGADOR' | 'AUDITOR' | 'DIRECTOR' | 'ALTO_RANGO' | 'ADMINISTRADOR';

export interface UsuarioSesion {
  nombre: string;
  rol: Rol;
  permisos: string[];
}

interface RespuestaLogin {
  token: string;
  venceEn: string;
  nombre: string;
  rol: Rol;
}

interface Sesion {
  usuario: UsuarioSesion | null;
  cargando: boolean;
  iniciar: (correo: string, contrasena: string) => Promise<void>;
  cerrar: () => void;
}

const SesionContext = createContext<Sesion | null>(null);

// El rol y los permisos siempre se piden al backend, no se guardan en el navegador
function cargarUsuario() {
  return pedir<UsuarioSesion>('/interno/auth/yo');
}

export function SesionProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioSesion | null>(null);
  const [cargando, setCargando] = useState(tieneToken);

  // Si recargan la página con sesión abierta, se recupera con el token
  useEffect(() => {
    if (!tieneToken()) return;
    cargarUsuario()
      .then(setUsuario)
      .catch(() => borrarToken())
      .finally(() => setCargando(false));
  }, []);

  async function iniciar(correo: string, contrasena: string) {
    const respuesta = await pedir<RespuestaLogin>('/interno/auth/login', {
      method: 'POST',
      body: JSON.stringify({ correo, contrasena }),
    });
    guardarToken(respuesta.token);
    setUsuario(await cargarUsuario());
  }

  function cerrar() {
    borrarToken();
    setUsuario(null);
  }

  return (
    <SesionContext.Provider value={{ usuario, cargando, iniciar, cerrar }}>
      {children}
    </SesionContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useSesion() {
  const sesion = useContext(SesionContext);
  if (!sesion) throw new Error('useSesion debe usarse dentro de SesionProvider');
  return sesion;
}
