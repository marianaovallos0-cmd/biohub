import { useEffect, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Leaf, Search } from 'lucide-react';
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

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-2">
        <h1 className="text-3xl font-extrabold tracking-tight text-bosque">Buscar especies y hallazgos</h1>
        <p className="text-muted-foreground">
          Consulta los hallazgos biológicos registrados en BioHub por nombre de especie.
          Por protección de las especies no se muestran coordenadas exactas.
        </p>
      </section>

      <form role="search" onSubmit={enviar} className="flex flex-col gap-3 sm:flex-row">
        <label htmlFor="busqueda" className="sr-only">Nombre de la especie</label>
        <Input
          id="busqueda"
          type="search"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          placeholder="Ej: Ara macao, Oophaga, gryphus"
          maxLength={100}
          autoFocus
          className="h-11 bg-card text-base"
        />
        <Button type="submit" disabled={cargando} className="h-11 px-6">
          <Search aria-hidden="true" />
          {cargando ? 'Buscando…' : 'Buscar'}
        </Button>
      </form>

      {error && (
        <Alert variant="destructive">
          <AlertTitle>No se pudo buscar</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {respuesta && !error && (
        respuesta.total === 0 ? (
          <Empty className="border bg-card">
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
          <Card>
            <CardHeader>
              <CardTitle>Resultados</CardTitle>
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
                        {r.departamento
                          ? `${r.departamento}${r.region ? ` · ${r.region}` : ''}`
                          : 'Sin ubicación'}
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
