import { useEffect, useState, type FormEvent } from 'react';
import { CalendarSearch, ShieldAlert } from 'lucide-react';
import { pedir } from '@/api/cliente';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

// Lo que devuelve GET /interno/intentos (HU-14)
interface Intento {
  id: string;
  fechaHora: string;
  usuario: string;
  correo: string;
  rol: string;
  recurso: string;
  ip: string | null;
}

const NOMBRE_ROL: Record<string, string> = {
  INVESTIGADOR: 'Investigador',
  AUDITOR: 'Auditor',
  DIRECTOR: 'Director',
  ALTO_RANGO: 'Alto rango',
  ADMINISTRADOR: 'Administrador',
};

const formatoFechaHora = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

// yyyy-mm-dd en hora local, que es lo que piden el input date y el backend
function aTextoFecha(fecha: Date) {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

function haceDias(dias: number) {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() - dias);
  return fecha;
}

export default function IntentosAcceso() {
  const [desde, setDesde] = useState(() => aTextoFecha(haceDias(7)));
  const [hasta, setHasta] = useState(() => aTextoFecha(new Date()));
  // Al entrar se muestran los de la última semana
  const [consulta, setConsulta] = useState(() => ({ desde, hasta }));
  const [resultado, setResultado] = useState<{ consulta: typeof consulta; intentos?: Intento[]; error?: string } | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;
    pedir<Intento[]>(`/interno/intentos?desde=${consulta.desde}&hasta=${consulta.hasta}`)
      .then((intentos) => { if (vigente) setResultado({ consulta, intentos }); })
      .catch(() => {
        if (vigente) setResultado({ consulta, error: 'No pudimos consultar los intentos. Intenta de nuevo en un momento.' });
      });
    return () => { vigente = false; };
  }, [consulta]);

  const actual = resultado?.consulta === consulta ? resultado : null;
  const cargando = !actual;
  const intentos = actual?.intentos ?? null;
  const error = aviso ?? actual?.error ?? null;

  function enviar(e: FormEvent) {
    e.preventDefault();
    if (desde > hasta) {
      setAviso('La fecha inicial no puede ser posterior a la fecha final.');
      return;
    }
    setAviso(null);
    setConsulta({ desde, hasta });
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-2">
        <h1 className="text-3xl font-extrabold tracking-tight text-bosque">Intentos de acceso no autorizado</h1>
        <p className="text-muted-foreground">
          Cada vez que alguien intenta entrar a algo que su rol no permite, queda registrado aquí.
        </p>
      </section>

      <Card className="rounded-3xl">
        <CardContent>
          <form onSubmit={enviar} className="flex flex-col gap-4 sm:flex-row sm:items-end">
            <div className="flex flex-col gap-2">
              <Label htmlFor="desde">Desde</Label>
              <Input id="desde" type="date" required value={desde} max={hasta}
                     onChange={(e) => setDesde(e.target.value)} className="h-11" />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="hasta">Hasta</Label>
              <Input id="hasta" type="date" required value={hasta} min={desde}
                     onChange={(e) => setHasta(e.target.value)} className="h-11" />
            </div>
            <Button type="submit" disabled={cargando} className="h-11 px-6">
              <CalendarSearch aria-hidden="true" />
              {cargando ? 'Consultando…' : 'Consultar'}
            </Button>
          </form>
        </CardContent>
      </Card>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {intentos && !error && (
        intentos.length === 0 ? (
          <Empty className="rounded-3xl border bg-card">
            <EmptyHeader>
              <EmptyMedia variant="icon"><ShieldAlert /></EmptyMedia>
              <EmptyTitle>No hay intentos en este periodo</EmptyTitle>
              <EmptyDescription>Nadie intentó entrar a algo que su rol no permite entre esas fechas.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <Card className="rounded-3xl">
            <CardHeader>
              <CardTitle className="text-xl font-extrabold text-bosque">Registro</CardTitle>
              <CardDescription aria-live="polite">
                {intentos.length} {intentos.length === 1 ? 'intento' : 'intentos'} en el periodo
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Fecha y hora</TableHead>
                    <TableHead>Usuario</TableHead>
                    <TableHead>Rol</TableHead>
                    <TableHead>Qué intentó</TableHead>
                    <TableHead>IP</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {intentos.map((intento) => (
                    <TableRow key={intento.id}>
                      <TableCell className="whitespace-nowrap">
                        {formatoFechaHora.format(new Date(intento.fechaHora))}
                      </TableCell>
                      <TableCell>
                        <p className="font-semibold text-bosque">{intento.usuario}</p>
                        <p className="text-sm text-muted-foreground">{intento.correo}</p>
                      </TableCell>
                      <TableCell><Badge variant="secondary">{NOMBRE_ROL[intento.rol] ?? intento.rol}</Badge></TableCell>
                      <TableCell className="font-mono text-sm">{intento.recurso}</TableCell>
                      <TableCell className="font-mono text-sm">{intento.ip ?? '—'}</TableCell>
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
