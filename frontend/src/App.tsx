import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { SesionProvider } from '@/auth/SesionContext';
import { RutaProtegida } from '@/auth/RutaProtegida';
import LayoutPublico from '@/layouts/LayoutPublico';
import LayoutInterno from '@/layouts/LayoutInterno';
import BusquedaPublica from '@/paginas/BusquedaPublica';
import FichaPublica from '@/paginas/FichaPublica';
import Login from '@/paginas/Login';
import InicioInterno from '@/paginas/InicioInterno';
import IntentosAcceso from '@/paginas/IntentosAcceso';
import RegistrarHallazgo from '@/paginas/RegistrarHallazgo';
import MisHallazgos from '@/paginas/MisHallazgos';
import TiposEspecie from '@/paginas/TiposEspecie';
import Reportes from '@/paginas/Reportes';
import UsuariosRoles from '@/paginas/UsuariosRoles';
import MapaUbicaciones from '@/paginas/MapaUbicaciones';
import EquiposCampo from '@/paginas/EquiposCampo';
import HallazgosAuditoria from '@/paginas/HallazgosAuditoria';
import HistorialHallazgo from '@/paginas/HistorialHallazgo';

export default function App() {
  return (
    <SesionProvider>
      <BrowserRouter>
        <Routes>
          {/* Zona pública: sin login (api-pública), HU-09 y HU-10 */}
          <Route element={<LayoutPublico />}>
            <Route path="/" element={<BusquedaPublica />} />
            <Route path="/fichas/:hallazgoId" element={<FichaPublica />} />
            <Route path="/login" element={<Login />} />
          </Route>

          {/* Zona interna: con login (api-interna) */}
          <Route element={<RutaProtegida />}>
            <Route element={<LayoutInterno />}>
              <Route path="/interno" element={<InicioInterno />} />
              {/* HU-13: cualquiera con sesión entra; solo alto rango recibe coordenadas */}
              <Route path="/interno/ubicaciones" element={<MapaUbicaciones />} />
            </Route>
          </Route>

          <Route element={<RutaProtegida roles={['INVESTIGADOR']} />}>
            <Route element={<LayoutInterno />}>
              <Route path="/interno/hallazgos/nuevo" element={<RegistrarHallazgo />} />
              <Route path="/interno/hallazgos/mios" element={<MisHallazgos />} />
              <Route path="/interno/tipos-especie" element={<TiposEspecie />} />
              </Route>
          </Route>
          <Route element={<RutaProtegida roles={['DIRECTOR']} />}>
            <Route element={<LayoutInterno />}>
              <Route path="/interno/reportes" element={<Reportes />} />
            </Route>
          </Route>

          <Route element={<RutaProtegida roles={['AUDITOR']} />}>
            <Route element={<LayoutInterno />}>
              <Route path="/interno/intentos" element={<IntentosAcceso />} />
              {/* HU-06, HU-07 y HU-08 */}
              <Route path="/interno/trazabilidad" element={<HallazgosAuditoria />} />
              <Route path="/interno/trazabilidad/:hallazgoId" element={<HistorialHallazgo />} />
            </Route>
          </Route>

          {/* HU-15: solo el administrador */}
          <Route element={<RutaProtegida roles={['ADMINISTRADOR']} />}>
            <Route element={<LayoutInterno />}>
              <Route path="/interno/usuarios" element={<UsuariosRoles />} />
              <Route path="/interno/equipos" element={<EquiposCampo />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </SesionProvider>
  );
}