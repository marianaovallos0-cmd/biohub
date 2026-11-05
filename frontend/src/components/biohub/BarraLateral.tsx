import { Link, NavLink, useNavigate } from 'react-router-dom';
import { Camera, ChartColumn, LayoutGrid, Leaf, LogOut, Search, ShieldAlert, Users, type LucideIcon } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useSesion, type Rol } from '@/auth/SesionContext';
import { Logo } from './Logo';

interface ItemMenu {
  ruta: string;
  texto: string;
  icono: LucideIcon;
}

// Qué ve cada rol en la barra lateral. Cada compañero agrega aquí el ítem de su HU.
const MENU: Record<Rol, ItemMenu[]> = {
  INVESTIGADOR: [
    { ruta: '/interno/hallazgos/nuevo', texto: 'Registrar hallazgo', icono: Leaf },
    { ruta: '/interno/tipos-especie', texto: 'Tipos de especie', icono: LayoutGrid },
  ],
  AUDITOR: [
    { ruta: '/interno/hallazgos', texto: 'Hallazgos', icono: Leaf },
    { ruta: '/interno/intentos', texto: 'Intentos de acceso', icono: ShieldAlert },
  ],
  DIRECTOR: [{ ruta: '/interno/reportes', texto: 'Reportes', icono: ChartColumn }],
  ALTO_RANGO: [{ ruta: '/interno/hallazgos', texto: 'Hallazgos', icono: Leaf }],
  ADMINISTRADOR: [
    { ruta: '/interno/usuarios', texto: 'Usuarios y roles', icono: Users },
    { ruta: '/interno/equipos', texto: 'Equipos de campo', icono: Camera },
  ],
};

const NOMBRE_ROL: Record<Rol, string> = {
  INVESTIGADOR: 'Investigador de campo',
  AUDITOR: 'Auditor científico',
  DIRECTOR: 'Director científico',
  ALTO_RANGO: 'Alto rango',
  ADMINISTRADOR: 'Administrador del sistema',
};

const claseItem = ({ isActive }: { isActive: boolean }) =>
  cn(
    'flex min-h-11 items-center gap-3 rounded-xl px-3 transition-colors',
    isActive
      ? 'bg-sidebar-accent font-bold text-sidebar-accent-foreground'
      : 'text-sidebar-foreground hover:bg-white/5',
  );

export function BarraLateral() {
  const { usuario, cerrar } = useSesion();
  const navegar = useNavigate();
  if (!usuario) return null;

  const iniciales = usuario.nombre.split(' ').map((p) => p[0]).slice(0, 2).join('');

  return (
    <aside className="flex flex-[1_1_248px] flex-col gap-6 bg-sidebar px-4 py-6 text-sidebar-foreground">
      <Link to="/interno" className="px-1.5">
        <Logo />
      </Link>

      <div className="flex items-center gap-3 rounded-2xl bg-white/5 p-3">
        <span aria-hidden="true" className="flex size-10 shrink-0 items-center justify-center rounded-full bg-brote font-bold text-bosque">
          {iniciales}
        </span>
        <div className="min-w-0">
          <p className="font-bold text-white">{usuario.nombre}</p>
          <p className="text-sm text-sidebar-foreground/80">{NOMBRE_ROL[usuario.rol]}</p>
        </div>
      </div>

      <nav aria-label="Menú interno" className="flex flex-col gap-1">
        {MENU[usuario.rol].map(({ ruta, texto, icono: Icono }) => (
          <NavLink key={ruta} to={ruta} className={claseItem}>
            <Icono aria-hidden="true" className="size-5" />
            {texto}
          </NavLink>
        ))}
        <NavLink to="/" className={claseItem}>
          <Search aria-hidden="true" className="size-5" />
          Búsqueda pública
        </NavLink>
      </nav>

      <button
        type="button"
        onClick={() => { cerrar(); navegar('/'); }}
        className="mt-auto flex min-h-11 items-center gap-3 rounded-xl px-3 text-sidebar-foreground hover:bg-white/5"
      >
        <LogOut aria-hidden="true" className="size-5" />
        Cerrar sesión
      </button>
    </aside>
  );
}