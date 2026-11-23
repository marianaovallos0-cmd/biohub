import { useEffect, useState } from 'react';
import { Camera } from 'lucide-react';
import { consultarEquiposDe, NOMBRE_TIPO_EQUIPO, type EquiposDeInvestigador, type Responsable } from '@/api/equipos';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

const claseSelect =
  'h-11 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50';

interface Props {
  responsables: Responsable[] | null;
  // Cambia cada vez que se registra un equipo, para volver a consultar
  version: number;
}

// HU-05. El administrador elige un investigador y ve de qué equipos responde
export function EquiposPorInvestigador({ responsables, version }: Props) {
  const [investigadorId, setInvestigadorId] = useState('');
  const [resultado, setResultado] = useState<{ id: string; datos?: EquiposDeInvestigador; error?: string } | null>(null);

  useEffect(() => {
    if (!investigadorId) return;
    let vigente = true;
    consultarEquiposDe(investigadorId)
      .then((datos) => { if (vigente) setResultado({ id: investigadorId, datos }); })
      .catch(() => { if (vigente) setResultado({ id: investigadorId, error: 'No pudimos consultar los equipos. Intenta de nuevo.' }); });
    return () => { vigente = false; };
  }, [investigadorId, version]);

  const actual = resultado?.id === investigadorId ? resultado : null;
  const cargando = !!investigadorId && !actual;

  return (
    <Card className="rounded-3xl">
      <CardHeader>
        <CardTitle className="text-xl font-extrabold text-bosque">Equipos por investigador</CardTitle>
        <CardDescription>Elige un investigador para ver de qué equipos responde.</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <div className="flex max-w-md flex-col gap-2">
          <Label htmlFor="consultar-investigador">Investigador</Label>
          <select id="consultar-investigador" value={investigadorId} disabled={!responsables}
                  onChange={(e) => setInvestigadorId(e.target.value)} className={claseSelect}>
            <option value="">{responsables ? 'Elige un investigador' : 'Cargando…'}</option>
            {responsables?.map((r) => <option key={r.id} value={r.id}>{r.nombre} ({r.correo})</option>)}
          </select>
        </div>

        <div aria-live="polite">
          {cargando && <p className="text-sm text-muted-foreground">Consultando…</p>}

          {actual?.error && (
            <Alert variant="destructive">
              <AlertDescription>{actual.error}</AlertDescription>
            </Alert>
          )}

          {actual?.datos && (
            actual.datos.equipos.length === 0 ? (
              <Empty className="rounded-2xl border">
                <EmptyHeader>
                  <EmptyMedia variant="icon"><Camera /></EmptyMedia>
                  <EmptyTitle>{actual.datos.investigador.nombre} no tiene equipos asignados</EmptyTitle>
                  <EmptyDescription>Puedes asignarle uno con el formulario de arriba.</EmptyDescription>
                </EmptyHeader>
              </Empty>
            ) : (
              <div className="flex flex-col gap-2">
                <p className="text-sm text-muted-foreground">
                  {actual.datos.investigador.nombre} responde por {actual.datos.equipos.length}{' '}
                  {actual.datos.equipos.length === 1 ? 'equipo' : 'equipos'}.
                </p>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Identificación</TableHead>
                      <TableHead>Tipo</TableHead>
                      <TableHead>Estado</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {actual.datos.equipos.map((e) => (
                      <TableRow key={e.id}>
                        <TableCell className="font-mono font-semibold">{e.codigoIdentificacion}</TableCell>
                        <TableCell>{NOMBRE_TIPO_EQUIPO[e.tipo]}</TableCell>
                        <TableCell><Badge variant="secondary">{e.estado}</Badge></TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )
          )}
        </div>
      </CardContent>
    </Card>
  );
}
