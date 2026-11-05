import { Navigate, Outlet } from 'react-router-dom';
import { useSesion, type Rol } from './SesionContext';

export function RutaProtegida({ roles }: { roles?: Rol[] }) {
  const { usuario } = useSesion();
  if (!usuario) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(usuario.rol)) return <Navigate to="/interno" replace />;
  return <Outlet />;
}