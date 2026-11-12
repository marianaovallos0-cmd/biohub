import { useEffect, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Leaf, MapPin, Search, ShieldCheck } from 'lucide-react';
import { pedir } from '@/api/cliente';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

// Lo que devuelve GET /publico/busqueda (HU-09)
interface ResultadoBusqueda {
  hallazgoId: string;
  nombreCientifico: string;
  tipoEspecie: string;
  clase: string | null;
  orden: string | null;
  familia: string | null;
  region: string | null;
  departamento: string | null;
  fechaRegistro: string;
}

interface RespuestaBusqueda {
  consulta: string;
  total: number;
  resultados: ResultadoBusqueda[];
  mensaje: string | null;
}

const MINIMO_CARACTERES = 2;

const formatoFecha = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });

export default function BusquedaPublica() {
  const [parametros, setParametros] = useSearchParams();
  const consulta = parametros.get('q') ?? '';
  const consultaValida = consulta.trim().length >= MINIMO_CARACTERES;
  const [texto, setTexto] = useState(consulta);
  const [consultaAnterior, setConsultaAnterior] = useState(consulta);
  const [aviso, setAviso] = useState<string | null>(null);
  // Resultado de la última búsqueda que respondió, con la consulta a la que pertenece.
  const [ultima, setUltima] = useState<{ consulta: string; respuesta?: RespuestaBusqueda; error?: string } | null>(null);

  // Si la URL cambia (atrás/adelante), la caja de texto muestra la consulta nueva.
  if (consulta !== consultaAnterior) {
    setConsultaAnterior(consulta);
    setTexto(consulta);
  }

  // La búsqueda vive en la URL (?q=...) para poder compartirla o volver atrás.
  useEffect(() => {
    if (!consultaValida) return;
    let vigente = true;
    pedir<RespuestaBusqueda>(`/publico/busqueda?q=${encodeURIComponent(consulta)}`)
      .then((respuesta) => { if (vigente) setUltima({ consulta, respuesta }); })
      .catch(() => {
        if (vigente) setUltima({ consulta, error: 'No pudimos hacer la búsqueda. Intenta de nuevo en un momento.' });
      });
    return () => { vigente = false; };
  }, [consulta, consultaValida]);

  const actual = consultaValida && ultima?.consulta === consulta ? ultima : null;
  const cargando = consultaValida && !actual;
  const respuesta = actual?.respuesta ?? null;
  const error = aviso ?? actual?.error ?? null;

  function enviar(e: FormEvent) {
    e.preventDefault();
    const limpio = texto.trim();
    if (limpio.length < MINIMO_CARACTERES) {
      setAviso(`Escribe al menos ${MINIMO_CARACTERES} letras del nombre de la especie.`);
      return;
    }
    setAviso(null);
    setParametros({ q: limpio });
  }

  function buscarEjemplo(ejemplo: string) {
    setAviso(null);
    setParametros({ q: ejemplo });
  }

  return (
    <div className="flex flex-col gap-8">
      {/* La franja verde ocupa todo el ancho y sigue desde el encabezado */}
      <section className="relative -mt-8 mx-[calc(50%-50vw)] overflow-hidden bg-bosque text-white">
        <Leaf aria-hidden="true" className="absolute right-[8%] -top-10 size-64 rotate-12 text-brote/10" />
        <Leaf aria-hidden="true" className="absolute -bottom-16 left-[6%] size-44 -rotate-45 text-brote/10" />

        <div className="relative mx-auto max-w-[1120px] px-6 pb-14 pt-10">
        <div className="relative flex max-w-2xl flex-col gap-3">
          <h1 className="text-3xl font-extrabold tracking-tight sm:text-4xl">Buscar especies y hallazgos</h1>
          <p className="text-white/75">
            Consulta los hallazgos biológicos registrados en BioHub por nombre de especie.
          </p>
        </div>

        <form role="search" onSubmit={enviar} className="relative mt-6 flex flex-col gap-3 sm:flex-row">
          <label htmlFor="busqueda" className="sr-only">Nombre de la especie</label>
          <Input
            id="busqueda"
            type="search"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            placeholder="Ej: Ara macao, Oophaga, gryphus"
            maxLength={100}
            autoFocus
            className="h-12 border-transparent bg-white text-base text-foreground"
          />
          <Button type="submit" disabled={cargando} className="h-12 bg-brote px-7 font-bold text-bosque hover:bg-brote/90">
            <Search aria-hidden="true" />
            {cargando ? 'Buscando…' : 'Buscar'}
          </Button>
        </form>

        <div className="relative mt-4 flex flex-wrap items-center gap-2 text-sm text-white/75">
          <span>Prueba con:</span>
          {['Ara macao', 'Oophaga histrionica', 'Vultur gryphus'].map((ejemplo) => (
            <button
              key={ejemplo}
              type="button"
              onClick={() => buscarEjemplo(ejemplo)}
              className="rounded-full border border-white/20 px-3 py-1 italic hover:bg-white/10"
            >
              {ejemplo}
            </button>
          ))}
        </div>

        <p className="relative mt-6 flex items-center gap-2 text-sm text-brote">
          <ShieldCheck aria-hidden="true" className="size-4" />
          Por protección de las especies no se muestran coordenadas exactas, solo la región y el departamento.
        </p>
        </div>
      </section>

      {!consultaValida && !error && (
        <Empty className="rounded-3xl border bg-card">
          <EmptyHeader>
            <EmptyMedia variant="icon"><Leaf /></EmptyMedia>
            <EmptyTitle>Escribe el nombre de una especie para empezar</EmptyTitle>
            <EmptyDescription>
              Puedes buscar por el nombre científico completo, solo el género o solo la especie.
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      )}

      {error && (
        <Alert variant="destructive">
          <AlertTitle>No se pudo buscar</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {respuesta && !error && (
        respuesta.total === 0 ? (
          <Empty className="rounded-3xl border bg-card">
            <EmptyHeader>
              <EmptyMedia variant="icon"><Leaf /></EmptyMedia>
              <EmptyTitle>Sin resultados</EmptyTitle>
              <EmptyDescription>
                {respuesta.mensaje ?? `No hay resultados para "${respuesta.consulta}".`} Revisa la
                ortografía o prueba solo con el género.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <Card className="rounded-3xl">
            <CardHeader>
              <CardTitle className="text-xl font-extrabold text-bosque">Resultados</CardTitle>
              <CardDescription aria-live="polite">
                {respuesta.total} {respuesta.total === 1 ? 'hallazgo' : 'hallazgos'} para “{respuesta.consulta}”
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Especie</TableHead>
                    <TableHead>Tipo</TableHead>
                    <TableHead>Familia</TableHead>
                    <TableHead>Ubicación</TableHead>
                    <TableHead>Registrado</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {respuesta.resultados.map((r) => (
                    <TableRow key={r.hallazgoId}>
                      <TableCell className="font-semibold italic text-bosque">{r.nombreCientifico}</TableCell>
                      <TableCell><Badge variant="secondary">{r.tipoEspecie}</Badge></TableCell>
                      <TableCell>{r.familia ?? '—'}</TableCell>
                      <TableCell>
                        {r.departamento ? (
                          <span className="inline-flex items-center gap-1.5">
                            <MapPin aria-hidden="true" className="size-4 text-muted-foreground" />
                            {r.departamento}{r.region ? ` · ${r.region}` : ''}
                          </span>
                        ) : 'Sin ubicación'}
                      </TableCell>
                      <TableCell className="whitespace-nowrap">{formatoFecha.format(new Date(r.fechaRegistro))}</TableCell>
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
