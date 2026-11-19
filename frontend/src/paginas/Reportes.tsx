import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { ChartColumn, LoaderCircle, RefreshCw, TriangleAlert } from 'lucide-react';
import {
  listarReportes,
  mesAnterior,
  nombreMes,
  pedirReporte,
  verReporte,
  type DetalleReporte,
  type ResumenReporte,
} from '@/api/reportes';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

const formatoFechaHora = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

// Mientras la analítica calcula, la pantalla vuelve a preguntar cada 10 segundos
const ESPERA_MS = 10_000;

function textoVariacion(variacion: number | null) {
  if (variacion === null) return 'Sin datos del mes anterior';
  return `${variacion > 0 ? '+' : ''}${variacion.toLocaleString('es-CO', { maximumFractionDigits: 2 })} %`;
}

// HU-18: el director pide el reporte de presión ecológica de un mes y lo consulta cuando la analítica lo genera
export default function Reportes() {
  const ultimoMes = mesAnterior();
  const [mes, setMes] = useState(ultimoMes);
  const [reportes, setReportes] = useState<ResumenReporte[] | null>(null);
  const [detalle, setDetalle] = useState<DetalleReporte | null>(null);
  const [pidiendo, setPidiendo] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const cargarLista = useCallback(() => {
    listarReportes()
      .then(setReportes)
      .catch(() => setError('No pudimos cargar los reportes. Intenta de nuevo en un momento.'));
  }, []);

  useEffect(() => {
    cargarLista();
  }, [cargarLista]);

  // Si el reporte abierto sigue pendiente, se revisa otra vez hasta que la analítica lo termine
  useEffect(() => {
    if (!detalle || detalle.generado) return;
    const espera = window.setTimeout(() => {
      verReporte(detalle.id)
        .then((actualizado) => {
          setDetalle(actualizado);
          if (actualizado.generado) cargarLista();
        })
        .catch(() => setError('No pudimos actualizar el reporte. Intenta de nuevo en un momento.'));
    }, ESPERA_MS);
    return () => window.clearTimeout(espera);
  }, [detalle, cargarLista]);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    // "AAAA-MM" se puede comparar como texto. El backend vuelve a validar
    if (mes > ultimoMes) {
      setError('Solo se pueden pedir meses que ya terminaron.');
      return;
    }
    setPidiendo(true);
    setError(null);
    try {
      setDetalle(await pedirReporte(mes));
      cargarLista();
    } catch {
      setError('No pudimos pedir el reporte. Revisa el mes e intenta de nuevo.');
    } finally {
      setPidiendo(false);
    }
  }

  async function abrir(id: string) {
    setError(null);
    try {
      setDetalle(await verReporte(id));
    } catch {
      setError('No pudimos abrir el reporte. Intenta de nuevo en un momento.');
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-2">
        <h1 className="text-3xl font-extrabold tracking-tight text-bosque">Reportes de presión ecológica</h1>
        <p className="text-muted-foreground">
          Hallazgos por departamento comparados con el mes anterior. Un departamento queda en alerta si sus hallazgos
          bajan 30 % o más.
        </p>
      </section>

      <Card className="rounded-3xl">
        <CardContent>
          <form onSubmit={enviar} className="flex flex-col gap-4 sm:flex-row sm:items-end">
            <div className="flex flex-col gap-2">
              <Label htmlFor="mes">Mes</Label>
              <Input id="mes" type="month" required max={ultimoMes} value={mes}
                     onChange={(e) => setMes(e.target.value)} className="h-11" />
            </div>
            <Button type="submit" disabled={pidiendo} className="h-11 px-6">
              <ChartColumn aria-hidden="true" />
              {pidiendo ? 'Pidiendo…' : 'Generar reporte'}
            </Button>
          </form>
          <p className="mt-3 text-sm text-muted-foreground">
            Solo se pueden pedir meses que ya terminaron. Si el mes ya se pidió, se abre el mismo reporte.
          </p>
        </CardContent>
      </Card>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {detalle && (
        <Card className="rounded-3xl">
          <CardHeader>
            <CardTitle className="text-xl font-extrabold capitalize text-bosque">{nombreMes(detalle.mes)}</CardTitle>
            <CardDescription aria-live="polite">
              {detalle.generado && detalle.fechaGeneracion
                ? `Generado el ${formatoFechaHora.format(new Date(detalle.fechaGeneracion))}`
                : 'Se está calculando. Tarda hasta un minuto y esta pantalla se actualiza sola.'}
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-6">
            {!detalle.generado ? (
              <p className="flex items-center gap-2 text-sm text-muted-foreground">
                <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
                Esperando a la analítica…
              </p>
            ) : (
              <>
                <dl className="grid gap-3 sm:grid-cols-3">
                  <div className="rounded-2xl border p-4">
                    <dt className="text-sm text-muted-foreground">Hallazgos del mes</dt>
                    <dd className="text-3xl font-extrabold text-bosque">{detalle.totalHallazgos}</dd>
                  </div>
                  <div className="rounded-2xl border p-4">
                    <dt className="text-sm text-muted-foreground">Especies protegidas</dt>
                    <dd className="text-3xl font-extrabold text-bosque">{detalle.hallazgosProtegidos}</dd>
                  </div>
                  <div className="rounded-2xl border p-4">
                    <dt className="text-sm text-muted-foreground">Departamentos en alerta</dt>
                    <dd className={`text-3xl font-extrabold ${detalle.departamentosEnAlerta > 0 ? 'text-destructive' : 'text-bosque'}`}>
                      {detalle.departamentosEnAlerta}
                    </dd>
                  </div>
                </dl>

                {detalle.filas.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    No hubo hallazgos en este mes ni en el anterior, así que todos los indicadores quedan en cero.
                  </p>
                ) : (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Departamento</TableHead>
                        <TableHead className="text-right">Hallazgos</TableHead>
                        <TableHead className="text-right">Protegidas</TableHead>
                        <TableHead className="text-right">Frente al mes anterior</TableHead>
                        <TableHead>Estado</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {detalle.filas.map((fila) => (
                        <TableRow key={fila.departamento}>
                          <TableCell className="font-semibold">{fila.departamento}</TableCell>
                          <TableCell className="text-right">{fila.totalHallazgos}</TableCell>
                          <TableCell className="text-right">{fila.hallazgosProtegidos}</TableCell>
                          <TableCell className="text-right">{textoVariacion(fila.variacionPorcentual)}</TableCell>
                          <TableCell>
                            {fila.enAlerta ? (
                              <Badge variant="destructive"><TriangleAlert aria-hidden="true" />En alerta</Badge>
                            ) : (
                              <Badge variant="secondary">Normal</Badge>
                            )}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </>
            )}
          </CardContent>
        </Card>
      )}

      <Card className="rounded-3xl">
        <CardHeader className="flex flex-row items-center justify-between gap-3">
          <div className="flex flex-col gap-1">
            <CardTitle className="text-xl font-extrabold text-bosque">Reportes pedidos</CardTitle>
            <CardDescription>Se abren tal como quedaron guardados, sin volver a calcularlos.</CardDescription>
          </div>
          <Button type="button" variant="outline" className="h-11 px-4" onClick={cargarLista}>
            <RefreshCw aria-hidden="true" />
            Actualizar
          </Button>
        </CardHeader>
        <CardContent>
          {!reportes && !error && <p className="text-sm text-muted-foreground">Cargando…</p>}
          {reportes?.length === 0 && (
            <Empty>
              <EmptyHeader>
                <EmptyMedia variant="icon"><ChartColumn /></EmptyMedia>
                <EmptyTitle>Todavía no hay reportes</EmptyTitle>
                <EmptyDescription>Elige un mes arriba y dale Generar reporte.</EmptyDescription>
              </EmptyHeader>
            </Empty>
          )}
          {reportes && reportes.length > 0 && (
            <ul className="flex flex-col gap-2">
              {reportes.map((reporte) => (
                <li key={reporte.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border p-3">
                  <div className="flex items-center gap-3">
                    <span className="font-semibold capitalize">{nombreMes(reporte.mes)}</span>
                    {reporte.generado
                      ? <Badge variant="secondary">Generado</Badge>
                      : <Badge variant="outline">Pendiente</Badge>}
                  </div>
                  <Button type="button" variant="outline" className="h-11 px-4" onClick={() => abrir(reporte.id)}>
                    Ver
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
