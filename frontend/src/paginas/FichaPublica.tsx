import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { ArrowLeft, Leaf, MapPin, ShieldCheck } from 'lucide-react';
import { pedir } from '@/api/cliente';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty';

// Lo que devuelve GET /publico/fichas/{id} (HU-10). No hay coordenadas: solo región y departamento.
interface UbicacionPublica {
  region: string;
  departamento: string;
}

interface Ficha {
  hallazgoId: string;
  nombreCientifico: string;
  tipoEspecie: string;
  reino: string | null;
  filo: string | null;
  clase: string | null;
  orden: string | null;
  familia: string | null;
  genero: string | null;
  especie: string | null;
  fechaRegistro: string;
  ubicacion: UbicacionPublica | null;
}

const formatoFecha = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'long', year: 'numeric' });

export default function FichaPublica() {
  const { hallazgoId = '' } = useParams();
  // Si llegó desde la búsqueda, el botón de volver regresa a esa misma búsqueda
  const busqueda = (useLocation().state as { busqueda?: string } | null)?.busqueda;
  const volverA = busqueda ? `/?q=${encodeURIComponent(busqueda)}` : '/';

  const [ultima, setUltima] = useState<{ id: string; ficha?: Ficha; error?: string } | null>(null);

  useEffect(() => {
    let vigente = true;
    pedir<Ficha>(`/publico/fichas/${encodeURIComponent(hallazgoId)}`)
      .then((ficha) => { if (vigente) setUltima({ id: hallazgoId, ficha }); })
      .catch((e: Error) => {
        if (!vigente) return;
        const noExiste = e.message.includes('404') || e.message.includes('400');
        setUltima({
          id: hallazgoId,
          error: noExiste
            ? 'Esta ficha no existe o el enlace está mal escrito.'
            : 'No pudimos cargar la ficha. Intenta de nuevo en un momento.',
        });
      });
    return () => { vigente = false; };
  }, [hallazgoId]);

  const actual = ultima?.id === hallazgoId ? ultima : null;
  const ficha = actual?.ficha ?? null;

  const taxonomia: [string, string | null][] = ficha
    ? [
        ['Reino', ficha.reino],
        ['Filo', ficha.filo],
        ['Clase', ficha.clase],
        ['Orden', ficha.orden],
        ['Familia', ficha.familia],
        ['Género', ficha.genero],
        ['Especie', ficha.especie],
      ]
    : [];

  return (
    <div className="flex flex-col gap-6">
      <Link to={volverA} className="inline-flex w-fit items-center gap-2 font-bold text-bosque hover:underline">
        <ArrowLeft aria-hidden="true" className="size-4" />
        Volver a la búsqueda
      </Link>

      {!actual && <p className="text-muted-foreground" aria-live="polite">Cargando la ficha…</p>}

      {actual?.error && (
        <Alert variant="destructive">
          <AlertTitle>No se pudo mostrar la ficha</AlertTitle>
          <AlertDescription>{actual.error}</AlertDescription>
        </Alert>
      )}

      {ficha && (
        <>
          <section className="flex flex-col gap-3">
            <Badge variant="secondary" className="w-fit">{ficha.tipoEspecie}</Badge>
            <h1 className="text-3xl font-extrabold italic tracking-tight text-bosque sm:text-4xl">
              {ficha.nombreCientifico}
            </h1>
            <p className="text-muted-foreground">
              Registrado el {formatoFecha.format(new Date(ficha.fechaRegistro))}
            </p>
          </section>

          <div className="grid gap-6 md:grid-cols-[2fr_1fr]">
            <Card className="rounded-3xl">
              <CardHeader>
                <CardTitle className="text-xl font-extrabold text-bosque">Clasificación taxonómica</CardTitle>
                <CardDescription>La clasificación vigente del hallazgo.</CardDescription>
              </CardHeader>
              <CardContent>
                <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2">
                  {taxonomia.map(([nivel, valor]) => (
                    <div key={nivel} className="contents">
                      <dt className="text-muted-foreground">{nivel}</dt>
                      <dd className={nivel === 'Género' || nivel === 'Especie' ? 'italic' : undefined}>
                        {valor ?? '—'}
                      </dd>
                    </div>
                  ))}
                </dl>
              </CardContent>
            </Card>

            <Card className="rounded-3xl">
              <CardHeader>
                <CardTitle className="text-xl font-extrabold text-bosque">Ubicación</CardTitle>
              </CardHeader>
              <CardContent className="flex flex-col gap-4">
                {ficha.ubicacion ? (
                  <p className="flex items-start gap-2">
                    <MapPin aria-hidden="true" className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
                    <span>
                      <span className="block font-bold">{ficha.ubicacion.departamento}</span>
                      <span className="text-muted-foreground">Región {ficha.ubicacion.region}</span>
                    </span>
                  </p>
                ) : (
                  <Empty className="p-0">
                    <EmptyHeader>
                      <EmptyMedia variant="icon"><Leaf /></EmptyMedia>
                      <EmptyTitle>Sin ubicación registrada</EmptyTitle>
                      <EmptyDescription>Este hallazgo todavía no tiene región ni departamento.</EmptyDescription>
                    </EmptyHeader>
                  </Empty>
                )}
                <p className="flex items-start gap-2 text-sm text-muted-foreground">
                  <ShieldCheck aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
                  Por protección de las especies no se muestran coordenadas exactas.
                </p>
              </CardContent>
            </Card>
          </div>
        </>
      )}
    </div>
  );
}
