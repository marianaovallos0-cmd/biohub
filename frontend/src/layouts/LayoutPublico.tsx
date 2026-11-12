import { Link, Outlet, useLocation } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { Logo } from '@/components/biohub/Logo';

export default function LayoutPublico() {
  const enLogin = useLocation().pathname === '/login';

  return (
    <div className="min-h-screen overflow-x-clip">
      <header className="bg-bosque">
        <nav aria-label="Principal" className="mx-auto flex max-w-[1120px] flex-wrap items-center justify-between gap-4 px-6 py-5">
          <Link to="/"><Logo /></Link>
          {enLogin ? (
            <Link to="/" className="inline-flex min-h-11 items-center gap-2 rounded-full px-5 font-bold text-brote hover:bg-white/5">
              <ArrowLeft aria-hidden="true" className="size-4" />
              Volver a la búsqueda
            </Link>
          ) : (
            <Link to="/login" className="inline-flex min-h-11 items-center rounded-full bg-brote px-5 font-bold text-bosque">
              Ingresar
            </Link>
          )}
        </nav>
      </header>
      <main className="mx-auto max-w-[1120px] px-6 py-8">
        <Outlet />
      </main>
    </div>
  );
}
