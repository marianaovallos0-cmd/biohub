import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ExternalLink, FolderOpen, Leaf } from 'lucide-react';
import { abrirEvidencia, listarMisHallazgos, NOMBRE_EVIDENCIA, type MiHallazgo } from '@/api/hallazgos';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button, buttonVariants } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty';

const formatoFechaHora = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

// HU-01, criterio 2: el investigador vuelve a ver sus hallazgos y abre las evidencias que adjuntó
export default function MisHallazgos() {
  const [hallazgos, setHallazgos] = useState<MiHallazgo[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [errorEvidencia, setErrorEvidencia] = useState<string | null>(null);

  useEffect(() => {
    listarMisHallazgos()
      .then(setHallazgos)
      .catch(() => setError('No pudimos cargar tus hallazgos. Intenta de nuevo en un momento.'));
  }, []);

  async function verEvidencia(id: string) {
    setErrorEvidencia(null);
    try {
      await abrirEvidencia(id);
    } catch {
      setErrorEvidencia('No pudimos abrir la evidencia. Intenta de nuevo en un momento.');
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-2">
        <h1 className="text-3xl font-extrabold tracking-tight text-bosque">Mis hallazgos</h1>
        <p className="text-muted-foreground">Los hallazgos que registraste, del más reciente al más antiguo, con sus evidencias.</p>
      </section>

      {(error || errorEvidencia) && (
        <Alert variant="destructive">
          <AlertDescription>{error ?? errorEvidencia}</AlertDescription>
        </Alert>
      )}

      {!hallazgos && !error && <p className="text-sm text-muted-foreground">Cargando…</p>}

      {hallazgos?.length === 0 && (
        <Empty className="rounded-3xl border bg-card">
          <EmptyHeader>
            <EmptyMedia variant="icon"><FolderOpen /></EmptyMedia>
            <EmptyTitle>Todavía no has registrado hallazgos</EmptyTitle>
            <EmptyDescription>Cuando registres uno, aparece aquí con sus evidencias.</EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Link to="/interno/hallazgos/nuevo" className={buttonVariants({ className: 'h-11 px-5' })}>
              <Leaf aria-hidden="true" />
              Registrar hallazgo
            </Link>
          </EmptyContent>
        </Empty>
      )}

      {hallazgos?.map((hallazgo) => (
        <Card key={hallazgo.id} className="rounded-3xl">
          <CardHeader>
            <CardTitle className="text-xl font-extrabold italic text-bosque">{hallazgo.especie}</CardTitle>
            <CardDescription>
              {formatoFechaHora.format(new Date(hallazgo.fechaRegistro))}
              {hallazgo.expedicion ? ` · ${hallazgo.expedicion}` : ''}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {hallazgo.evidencias.length === 0 ? (
              <p className="text-sm text-muted-foreground">Este hallazgo no tiene evidencias.</p>
            ) : (
              <ul className="flex flex-col gap-2">
                {hallazgo.evidencias.map((ev) => (
                  <li key={ev.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border p-3">
                    <div className="min-w-0">
                      <p className="font-semibold">{NOMBRE_EVIDENCIA[ev.tipo]}</p>
                      {ev.descripcion && <p className="text-sm text-muted-foreground">{ev.descripcion}</p>}
                    </div>
                    <Button type="button" variant="outline" className="h-11 px-4" onClick={() => verEvidencia(ev.id)}>
                      <ExternalLink aria-hidden="true" />
                      Ver
                    </Button>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
