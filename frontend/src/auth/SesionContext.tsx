import { createContext, useContext, useState, type ReactNode } from 'react';

export type Rol = 'INVESTIGADOR' | 'AUDITOR' | 'DIRECTOR' | 'ALTO_RANGO' | 'ADMINISTRADOR';

export interface UsuarioSesion {
  nombre: string;
  rol: Rol;
}

interface Sesion {
  usuario: UsuarioSesion | null;
  iniciar: (usuario: UsuarioSesion) => void;
  cerrar: () => void;
}

const SesionContext = createContext<Sesion | null>(null);

export function SesionProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioSesion | null>(null);
  return (
    <SesionContext.Provider value={{ usuario, iniciar: setUsuario, cerrar: () => setUsuario(null) }}>
      {children}
    </SesionContext.Provider>
  );
}

export function useSesion() {
  const sesion = useContext(SesionContext);
  if (!sesion) throw new Error('useSesion debe usarse dentro de SesionProvider');
  return sesion;
}