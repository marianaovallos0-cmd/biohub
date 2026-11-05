import { Link, Outlet } from 'react-router-dom';
import { Logo } from '@/components/biohub/Logo';

export default function LayoutPublico() {
  return (
    <div className="min-h-screen">
      <header className="bg-bosque">
        <nav aria-label="Principal" className="mx-auto flex max-w-[1120px] flex-wrap items-center justify-between gap-4 px-6 py-5">
          <Link to="/"><Logo /></Link>
          <Link to="/login" className="inline-flex min-h-11 items-center rounded-full bg-brote px-5 font-bold text-bosque">
            Ingresar
          </Link>
        </nav>
      </header>
      <main className="mx-auto max-w-[1120px] px-6 py-8">
        <Outlet />
      </main>
    </div>
  );
}