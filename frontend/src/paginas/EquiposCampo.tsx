import { useEffect, useState, type FormEvent } from 'react';
import { Camera, CircleCheck, Plus } from 'lucide-react';
import { ErrorApi } from '@/api/cliente';
import {
  listarEquipos,
  listarResponsables,
  NOMBRE_TIPO_EQUIPO,
  registrarEquipo,
  type Equipo,
  type Responsable,
  type TipoEquipo,
} from '@/api/equipos';
import { EquiposPorInvestigador } from '@/components/biohub/EquiposPorInvestigador';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

const claseSelect =
  'h-11 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50';

// HU-04. El administrador registra cada equipo de campo con su investigador responsable.
export default function EquiposCampo() {
  const [equipos, setEquipos] = useState<Equipo[] | null>(null);
  const [responsables, setResponsables] = useState<Responsable[] | null>(null);
  const [errorCarga, setErrorCarga] = useState<string | null>(null);

  const [codigo, setCodigo] = useState('');
  const [tipo, setTipo] = useState<TipoEquipo>('DRON');
  const [investigadorId, setInvestigadorId] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [errores, setErrores] = useState<string[]>([]);
  const [registrado, setRegistrado] = useState<Equipo | null>(null);
  const [version, setVersion] = useState(0);

  function cargarEquipos() {
    return listarEquipos()
      .then(setEquipos)
      .catch(() => setErrorCarga('No pudimos cargar los equipos. Intenta de nuevo en un momento.'));
  }

  useEffect(() => {
    cargarEquipos();
    listarResponsables()
      .then(setResponsables)
      .catch(() => setErrorCarga('No pudimos cargar los investigadores. Intenta de nuevo en un momento.'));
  }, []);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setEnviando(true);
    setErrores([]);
    setRegistrado(null);
    try {
      const equipo = await registrarEquipo({ codigoIdentificacion: codigo, tipo, investigadorId: investigadorId || null });
      setRegistrado(equipo);
      setVersion((v) => v + 1);
      setCodigo('');
      setInvestigadorId('');
      await cargarEquipos();
    } catch (err) {
      if (err instanceof ErrorApi && err.errores.length > 0) setErrores(err.errores);
      else if (err instanceof ErrorApi && err.detalle) setErrores([err.detalle]);
      else if (err instanceof ErrorApi && err.estado === 403) setErrores(['Tu rol no tiene permiso para registrar equipos.']);
      else setErrores(['No pudimos registrar el equipo. Intenta de nuevo en un momento.']);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-2">
        <h1 className="text-3xl font-extrabold tracking-tight text-bosque">Equipos de campo</h1>
        <p className="text-muted-foreground">
          Cada dron, cámara trampa, hidrófono o sensor queda a cargo de un investigador. Ningún equipo queda sin responsable.
        </p>
      </section>

      {errorCarga && (
        <Alert variant="destructive">
          <AlertDescription>{errorCarga}</AlertDescription>
        </Alert>
      )}

      <Card className="rounded-3xl">
        <CardHeader>
          <CardTitle className="text-xl font-extrabold text-bosque">Registrar un equipo</CardTitle>
          <CardDescription>La identificación no se puede repetir. Los campos con * son obligatorios.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={enviar} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-[1fr_200px_1fr_auto] lg:items-end">
            <div className="flex flex-col gap-2">
              <Label htmlFor="codigo">Identificación<span className="text-destructive" aria-hidden="true">*</span></Label>
              <Input id="codigo" required maxLength={50} value={codigo} placeholder="Ej.: DRN-002"
                     onChange={(e) => setCodigo(e.target.value)} className="h-11 font-mono uppercase" />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="tipo">Tipo<span className="text-destructive" aria-hidden="true">*</span></Label>
              <select id="tipo" value={tipo} onChange={(e) => setTipo(e.target.value as TipoEquipo)} className={claseSelect}>
                {(Object.keys(NOMBRE_TIPO_EQUIPO) as TipoEquipo[]).map((t) => (
                  <option key={t} value={t}>{NOMBRE_TIPO_EQUIPO[t]}</option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="responsable">Investigador responsable<span className="text-destructive" aria-hidden="true">*</span></Label>
              <select id="responsable" required value={investigadorId} disabled={!responsables}
                      onChange={(e) => setInvestigadorId(e.target.value)} className={claseSelect}>
                <option value="">{responsables ? 'Elige un investigador' : 'Cargando…'}</option>
                {responsables?.map((r) => <option key={r.id} value={r.id}>{r.nombre} ({r.correo})</option>)}
              </select>
            </div>
            <Button type="submit" disabled={enviando} className="h-11 px-6">
              <Plus aria-hidden="true" />
              {enviando ? 'Registrando…' : 'Registrar'}
            </Button>
          </form>

          <div aria-live="polite" className="mt-4 flex flex-col gap-3">
            {errores.length > 0 && (
              <Alert variant="destructive">
                <AlertTitle>El equipo no se registró</AlertTitle>
                <AlertDescription>
                  <ul className="list-disc pl-5">
                    {errores.map((error) => <li key={error}>{error}</li>)}
                  </ul>
                </AlertDescription>
              </Alert>
            )}
            {registrado && (
              <Alert>
                <CircleCheck aria-hidden="true" />
                <AlertDescription>
                  {NOMBRE_TIPO_EQUIPO[registrado.tipo]} <span className="font-mono font-semibold">{registrado.codigoIdentificacion}</span>{' '}
                  registrado a cargo de {registrado.responsable}.
                </AlertDescription>
              </Alert>
            )}
          </div>
        </CardContent>
      </Card>

      <EquiposPorInvestigador responsables={responsables} version={version} />

      {equipos && (
        equipos.length === 0 ? (
          <Empty className="rounded-3xl border bg-card">
            <EmptyHeader>
              <EmptyMedia variant="icon"><Camera /></EmptyMedia>
              <EmptyTitle>Todavía no hay equipos registrados</EmptyTitle>
              <EmptyDescription>Registra el primero con el formulario de arriba.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <Card className="rounded-3xl">
            <CardHeader>
              <CardTitle className="text-xl font-extrabold text-bosque">Equipos registrados</CardTitle>
              <CardDescription>{equipos.length} {equipos.length === 1 ? 'equipo' : 'equipos'}</CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Identificación</TableHead>
                    <TableHead>Tipo</TableHead>
                    <TableHead>Responsable</TableHead>
                    <TableHead>Estado</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {equipos.map((e) => (
                    <TableRow key={e.id}>
                      <TableCell className="font-mono font-semibold">{e.codigoIdentificacion}</TableCell>
                      <TableCell>{NOMBRE_TIPO_EQUIPO[e.tipo]}</TableCell>
                      <TableCell>{e.responsable}</TableCell>
                      <TableCell><Badge variant="secondary">{e.estado}</Badge></TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        )
      )}
    </div>
  );
}
