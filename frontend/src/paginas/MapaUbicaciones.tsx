import { useEffect, useState } from 'react';
import { CircleMarker, MapContainer, Popup, TileLayer } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { Lock, MapPin } from 'lucide-react';
import { pedir } from '@/api/cliente';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

// Lo que devuelve GET /interno/ubicaciones (HU-13).
// latitud y longitud solo llegan si el usuario tiene VER_COORDENADAS (alto rango).
interface Ubicacion {
  hallazgoId: string;
  nombreCientifico: string;
  region: string;
  departamento: string;
  especieProtegida: boolean;
  exacta: boolean;
  latitud?: number;
  longitud?: number;
}

const CENTRO_COLOMBIA: [number, number] = [4.6, -74.1];

export default function MapaUbicaciones() {
  const [ubicaciones, setUbicaciones] = useState<Ubicacion[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    pedir<Ubicacion[]>('/interno/ubicaciones')
      .then(setUbicaciones)
      .catch(() => setError('No pudimos cargar las ubicaciones. Intenta de nuevo en un momento.'));
  }, []);

  // Solo se dibuja el mapa con puntos que traen coordenadas de verdad (nunca aproximadas)
  const conCoordenadas = (ubicaciones ?? []).filter(
    (u): u is Ubicacion & { latitud: number; longitud: number } =>
      u.exacta && typeof u.latitud === 'number' && typeof u.longitud === 'number',
  );
  const puedeVerMapa = ubicaciones !== null && conCoordenadas.length > 0;

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-2">
        <h1 className="text-3xl font-extrabold tracking-tight text-bosque">Ubicación de los hallazgos</h1>
        <p className="text-muted-foreground">
          Las coordenadas exactas solo las ve el personal científico de alto rango.
        </p>
      </section>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {!ubicaciones && !error && <p className="text-muted-foreground" aria-live="polite">Cargando…</p>}

      {ubicaciones && !puedeVerMapa && (
        <Alert>
          <Lock aria-hidden="true" />
          <AlertTitle>Sin acceso a coordenadas exactas</AlertTitle>
          <AlertDescription>
            Tu rol solo puede ver la región y el departamento de cada hallazgo. Para proteger a las especies, el
            mapa con coordenadas es solo para el personal de alto rango.
          </AlertDescription>
        </Alert>
      )}

      {puedeVerMapa && (
        <Card className="overflow-hidden rounded-3xl">
          <CardHeader>
            <CardTitle className="text-xl font-extrabold text-bosque">Mapa</CardTitle>
            <CardDescription>
              {conCoordenadas.length} hallazgo(s) con coordenadas. En rojo, especies protegidas.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <MapContainer
              center={CENTRO_COLOMBIA}
              zoom={5}
              scrollWheelZoom={false}
              className="h-[28rem] w-full rounded-2xl"
            >
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              {conCoordenadas.map((u) => (
                <CircleMarker
                  key={u.hallazgoId}
                  center={[u.latitud, u.longitud]}
                  radius={9}
                  pathOptions={{
                    color: u.especieProtegida ? '#b91c1c' : '#1f4d3a',
                    fillColor: u.especieProtegida ? '#ef4444' : '#4f8a5b',
                    fillOpacity: 0.8,
                  }}
                >
                  <Popup>
                    <strong><em>{u.nombreCientifico}</em></strong>
                    <br />
                    {u.departamento} · {u.region}
                    <br />
                    {u.latitud.toFixed(4)}, {u.longitud.toFixed(4)}
                  </Popup>
                </CircleMarker>
              ))}
            </MapContainer>
          </CardContent>
        </Card>
      )}

      {ubicaciones && (
        <Card className="rounded-3xl">
          <CardHeader>
            <CardTitle className="text-xl font-extrabold text-bosque">Hallazgos</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Especie</TableHead>
                  <TableHead>Ubicación</TableHead>
                  {puedeVerMapa && <TableHead>Coordenadas</TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {ubicaciones.map((u) => (
                  <TableRow key={u.hallazgoId}>
                    <TableCell>
                      <span className="font-semibold italic text-bosque">{u.nombreCientifico || 'Sin nombre'}</span>
                      {u.especieProtegida && <Badge variant="destructive" className="ml-2">Protegida</Badge>}
                    </TableCell>
                    <TableCell>
                      <span className="inline-flex items-center gap-1">
                        <MapPin aria-hidden="true" className="size-4 text-muted-foreground" />
                        {u.departamento} · {u.region}
                      </span>
                    </TableCell>
                    {puedeVerMapa && (
                      <TableCell className="font-mono text-sm">
                        {u.exacta && typeof u.latitud === 'number' && typeof u.longitud === 'number'
                          ? `${u.latitud.toFixed(4)}, ${u.longitud.toFixed(4)}`
                          : '—'}
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
