import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { SesionProvider } from '@/auth/SesionContext';
import { RutaProtegida } from '@/auth/RutaProtegida';
import LayoutPublico from '@/layouts/LayoutPublico';
import LayoutInterno from '@/layouts/LayoutInterno';

export default function App() {
  return (
    <SesionProvider>
      <BrowserRouter>
        <Routes>
          {/* Zona pública: sin login (api-pública). Aquí van las páginas de HU-09 y HU-10 */}
          <Route element={<LayoutPublico />}>
            <Route path="/" element={null} />
          </Route>

          {/* Zona interna: con login (api-interna). Cada HU agrega su página aquí */}
          <Route element={<RutaProtegida />}>
            <Route element={<LayoutInterno />}>
              <Route path="/interno" element={null} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </SesionProvider>
  );
}