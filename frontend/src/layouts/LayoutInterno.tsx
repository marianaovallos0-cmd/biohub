import { Outlet } from 'react-router-dom';
import { BarraLateral } from '@/components/biohub/BarraLateral';

export default function LayoutInterno() {
  return (
    <div className="flex min-h-screen flex-wrap">
      <BarraLateral />
      <main className="flex-[999_1_480px] px-6 py-8">
        <Outlet />
      </main>
    </div>
  );
}
