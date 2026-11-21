import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, GitBranch, History, ShieldCheck } from 'lucide-react';
import { ErrorApi } from '@/api/cliente';
import { listarTiposEspecie, nombreLegible, type TipoEspecie } from '@/api/taxonomia';
import {
  consultarHistorial,
  JUSTIFICACION_MAXIMA,
  JUSTIFICACION_MINIMA,
  reclasificarHallazgo,
  type HistorialHallazgo as Historial,
  type ReclasificacionRegistrada,
  type VersionFicha,
} from '@/api/trazabilidad';
import { CamposAtributos } from '@/components/biohub/CamposAtributos';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button, buttonVariants } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { cn } from '@/lib/utils';

const RANGOS = [
  { campo: 'reino', texto: 'Reino', obligatorio: false },
  { campo: 'filo', texto: 'Filo', obligatorio: false },
  { campo: 'clase', texto: 'Clase', obligatorio: false },
  { campo: 'orden', texto: 'Orden', obligatorio: false },
  { campo: 'familia', texto: 'Familia', obligatorio: false },
  { campo: 'genero', texto: 'Género', obligatorio: true },
  { campo: 'especie', texto: 'Especie', obligatorio: true },
] as const;

type Rango = (typeof RANGOS)[number]['campo'];

const claseCampo =
  'w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50';

const formatoFechaHora = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

// HU-08: el historial completo. HU-06 y HU-07: reclasificar con justificación.
export default function HistorialHallazgo() {
  const { hallazgoId = '' } = useParams();
  const [historial, setHistorial] = useState<Historial | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [seleccionada, setSeleccionada] = useState<number | null>(null);
  const [reclasificando, setReclasificando] = useState(false);
  const [exito, setExito] = useState<ReclasificacionRegistrada | null>(null);

  const cargar = useCallback(() => {
    return consultarHistorial(hallazgoId)
      .then((h) => {
        setHistorial(h);
        setSeleccionada(h.versionVigente);
      })
      .catch((e) => setError(e instanceof ErrorApi && e.estado === 404
        ? 'Este hallazgo no existe.'
        : 'No pudimos cargar el historial. Intenta de nuevo en un momento.'));
  }, [hallazgoId]);

  useEffect(() => { void cargar(); }, [cargar]);

  const version = historial?.versiones.find((v) => v.numeroVersion === seleccionada) ?? null;
  const vigente = historial?.versiones.find((v) => v.vigente) ?? null;

  function alReclasificar(r: ReclasificacionRegistrada) {
    setReclasificando(false);
    setExito(r);
    void cargar();
  }

  return (
    <div className="flex flex-col gap-6">
      <Link to="/interno/trazabilidad" className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-primary hover:underline">
        <ArrowLeft aria-hidden="true" className="size-4" />
        Volver a hallazgos
      </Link>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {!historial && !error && <p className="text-sm text-muted-foreground">Cargando…</p>}

      {historial && vigente && (
        <>
          <section className="flex flex-wrap items-end justify-between gap-4">
            <div className="flex flex-col gap-2">
              <p className="text-sm font-semibold text-muted-foreground">Clasificación vigente</p>
              <h1 className="text-3xl font-extrabold tracking-tight text-bosque italic">
                {vigente.genero} {vigente.especie}
              </h1>
              <p className="text-muted-foreground">
                Registrado por {historial.investigador} el {formatoFechaHora.format(new Date(historial.fechaRegistro))}
                {historial.expedicion ? ` · ${historial.expedicion}` : ''}
              </p>
            </div>
            {!reclasificando && (
              <Button type="button" className="h-11 px-5" onClick={() => { setExito(null); setReclasificando(true); }}>
                <GitBranch aria-hidden="true" />
                Reclasificar
              </Button>
            )}
          </section>

          {historial.versionVigente > 1 && (
            <Alert className="border-ambar bg-ambar text-ambar-foreground">
              <History aria-hidden="true" />
              <AlertTitle>Este hallazgo tiene versiones anteriores</AlertTitle>
              <AlertDescription className="text-ambar-foreground">
                Se reclasificó {historial.versionVigente - 1} {historial.versionVigente === 2 ? 'vez' : 'veces'}.
                Ninguna versión se borra: puedes ver cada una tal como estaba.
              </AlertDescription>
            </Alert>
          )}

          {exito && (
            <Alert className="border-exito bg-exito text-exito-foreground">
              <CheckCircle2 aria-hidden="true" />
              <AlertTitle>Reclasificación guardada</AlertTitle>
              <AlertDescription className="text-exito-foreground">
                Pasó de <em>{exito.especieAnterior}</em> (v{exito.versionAnterior}) a <em>{exito.especieNueva}</em>{' '}
                (v{exito.versionNueva}). Quedó registrado que la autorizó {exito.autorizadoPor}.
              </AlertDescription>
            </Alert>
          )}

          {reclasificando && (
            <FormularioReclasificacion
              hallazgoId={historial.hallazgoId}
              vigente={vigente}
              onCancelar={() => setReclasificando(false)}
              onGuardado={alReclasificar}
            />
          )}

          <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
            <Card className="h-fit rounded-3xl">
              <CardHeader>
                <CardTitle className="text-lg font-extrabold text-bosque">Versiones</CardTitle>
                <CardDescription>De la más antigua a la vigente.</CardDescription>
              </CardHeader>
              <CardContent>
                <ol className="flex flex-col gap-2" aria-label="Versiones del hallazgo">
                  {historial.versiones.map((v) => (
                    <li key={v.id}>
                      <button
                        type="button"
                        aria-current={v.numeroVersion === seleccionada ? 'true' : undefined}
                        onClick={() => setSeleccionada(v.numeroVersion)}
                        className={cn(
                          'flex w-full flex-col items-start gap-1 rounded-2xl border p-3 text-left transition-colors',
                          v.numeroVersion === seleccionada ? 'border-primary bg-accent' : 'hover:bg-muted',
                        )}
                      >
                        <span className="flex w-full items-center justify-between gap-2">
                          <span className="font-bold text-bosque">Versión {v.numeroVersion}</span>
                          {v.vigente && <Badge>Vigente</Badge>}
                        </span>
                        <span className="text-sm italic">{v.genero} {v.especie}</span>
                        <span className="text-xs text-muted-foreground">{formatoFechaHora.format(new Date(v.fechaVersion))}</span>
                      </button>
                    </li>
                  ))}
                </ol>
              </CardContent>
            </Card>

            {version && <DetalleVersion version={version} />}
          </div>
        </>
      )}
    </div>
  );
}

// HU-08, criterio 2: los datos tal como estaban en esa versión
function DetalleVersion({ version }: { version: VersionFicha }) {
  return (
    <Card className="rounded-3xl">
      <CardHeader>
        <CardTitle className="flex flex-wrap items-center gap-3 text-xl font-extrabold text-bosque">
          Versión {version.numeroVersion}
          {version.vigente ? <Badge>Vigente</Badge> : <Badge variant="secondary">Histórica</Badge>}
        </CardTitle>
        <CardDescription>
          {version.numeroVersion === 1 ? 'Registro original' : 'Reclasificación'} del{' '}
          {formatoFechaHora.format(new Date(version.fechaVersion))} · {version.tipoEspecie}
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-6">
        <dl className="grid gap-x-6 gap-y-3 sm:grid-cols-2 lg:grid-cols-4">
          {RANGOS.map(({ campo, texto }) => (
            <div key={campo}>
              <dt className="text-xs font-semibold text-muted-foreground">{texto}</dt>
              <dd className={campo === 'genero' || campo === 'especie' ? 'italic' : undefined}>
                {version[campo] ?? '—'}
              </dd>
            </div>
          ))}
        </dl>

        <div className="flex flex-col gap-2">
          <h2 className="font-bold text-bosque">Atributos de esta versión</h2>
          {version.atributos.length === 0 ? (
            <p className="text-sm text-muted-foreground">Esta versión no tiene atributos registrados.</p>
          ) : (
            <dl className="grid gap-x-6 gap-y-3 sm:grid-cols-2">
              {version.atributos.map((a) => (
                <div key={a.nombre}>
                  <dt className="text-xs font-semibold text-muted-foreground">{nombreLegible(a.nombre)}</dt>
                  <dd className="break-all">{a.tipoDato === 'ARCHIVO' ? a.valor.split('/').pop() : a.valor}</dd>
                </div>
              ))}
            </dl>
          )}
        </div>

        {version.cambio && (
          <div className="flex flex-col gap-2 rounded-2xl border bg-muted p-4">
            <h2 className="flex items-center gap-2 font-bold text-bosque">
              <ShieldCheck aria-hidden="true" className="size-5" />
              Autorización del cambio
            </h2>
            <p className="text-sm">
              Cambió la versión {version.cambio.versionAnterior}. Lo autorizó <strong>{version.cambio.autorizadoPor}</strong> el{' '}
              {formatoFechaHora.format(new Date(version.cambio.fechaHora))}.
            </p>
            <blockquote className="border-l-4 border-primary pl-3 text-sm">{version.cambio.justificacion}</blockquote>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

interface PropsFormulario {
  hallazgoId: string;
  vigente: VersionFicha;
  onCancelar: () => void;
  onGuardado: (r: ReclasificacionRegistrada) => void;
}

// HU-06 y HU-07: parte de la clasificación vigente y exige justificación
function FormularioReclasificacion({ hallazgoId, vigente, onCancelar, onGuardado }: PropsFormulario) {
  const [tipos, setTipos] = useState<TipoEspecie[] | null>(null);
  const [tipoId, setTipoId] = useState('');
  const [rangos, setRangos] = useState<Record<Rango, string>>(() => ({
    reino: vigente.reino ?? '', filo: vigente.filo ?? '', clase: vigente.clase ?? '', orden: vigente.orden ?? '',
    familia: vigente.familia ?? '', genero: vigente.genero, especie: vigente.especie,
  }));
  const [valores, setValores] = useState<Record<string, string>>({});
  const [justificacion, setJustificacion] = useState('');
  const [errores, setErrores] = useState<string[]>([]);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    listarTiposEspecie()
      .then((lista) => {
        setTipos(lista);
        const actual = lista.find((t) => t.nombre === vigente.tipoEspecie);
        if (actual) {
          setTipoId(actual.id);
          setValores(valoresDe(actual, vigente));
        }
      })
      .catch(() => setErrores(['No pudimos cargar los tipos de especie.']));
  }, [vigente]);

  const tipo = tipos?.find((t) => t.id === tipoId) ?? null;
  // Los archivos no se suben de nuevo: si el tipo no cambia, la versión nueva conserva los de la vigente
  const atributosEditables = tipo?.atributos.filter((a) => a.tipoDato !== 'ARCHIVO') ?? [];
  const hayArchivos = (tipo?.atributos.length ?? 0) > atributosEditables.length;
  const largo = justificacion.trim().length;

  function elegirTipo(id: string) {
    setTipoId(id);
    const nuevo = tipos?.find((t) => t.id === id);
    setValores(nuevo && nuevo.nombre === vigente.tipoEspecie ? valoresDe(nuevo, vigente) : {});
  }

  async function enviar(e: FormEvent) {
    e.preventDefault();
    if (largo < JUSTIFICACION_MINIMA) {
      setErrores([`La justificación debe tener al menos ${JUSTIFICACION_MINIMA} caracteres.`]);
      return;
    }
    setEnviando(true);
    setErrores([]);
    try {
      const r = await reclasificarHallazgo(hallazgoId, {
        justificacion: justificacion.trim(),
        ficha: { tipoEspecieId: tipoId, ...rangos, valores },
      });
      onGuardado(r);
    } catch (error) {
      if (error instanceof ErrorApi && error.estado === 409) {
        setErrores([error.detalle ?? 'Otra persona reclasificó este hallazgo al mismo tiempo.']);
      } else if (error instanceof ErrorApi && error.errores.length > 0) {
        setErrores(error.errores);
      } else {
        setErrores(['No pudimos guardar la reclasificación. Intenta de nuevo en un momento.']);
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Card className="rounded-3xl border-primary">
      <CardHeader>
        <CardTitle className="text-xl font-extrabold text-bosque">Reclasificar hallazgo</CardTitle>
        <CardDescription>
          Se crea la versión {vigente.numeroVersion + 1}. La versión {vigente.numeroVersion} se conserva tal como está.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={enviar} className="flex flex-col gap-6" noValidate>
          {errores.length > 0 && (
            <Alert variant="destructive">
              <AlertTitle>No se guardó la reclasificación</AlertTitle>
              <AlertDescription>
                <ul className="list-disc pl-5">{errores.map((er) => <li key={er}>{er}</li>)}</ul>
              </AlertDescription>
            </Alert>
          )}

          <div className="flex max-w-sm flex-col gap-2">
            <Label htmlFor="r-tipo">Tipo de especie<span className="text-destructive" aria-hidden="true">*</span></Label>
            <select id="r-tipo" required value={tipoId} onChange={(e) => elegirTipo(e.target.value)}
                    disabled={!tipos} className={cn(claseCampo, 'h-11')}>
              <option value="">{tipos ? 'Elige un tipo' : 'Cargando…'}</option>
              {tipos?.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
            </select>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {RANGOS.map(({ campo, texto, obligatorio }) => (
              <div key={campo} className="flex flex-col gap-2">
                <Label htmlFor={`r-${campo}`}>
                  {texto}
                  {obligatorio && <span className="text-destructive" aria-hidden="true">*</span>}
                </Label>
                <Input id={`r-${campo}`} required={obligatorio} maxLength={100} value={rangos[campo]}
                       className={campo === 'genero' || campo === 'especie' ? 'h-11 italic' : 'h-11'}
                       onChange={(e) => setRangos((r) => ({ ...r, [campo]: e.target.value }))} />
              </div>
            ))}
          </div>

          {tipo && (
            <div className="flex flex-col gap-3">
              <h2 className="font-bold text-bosque">Atributos de {tipo.nombre.toLowerCase()}</h2>
              <CamposAtributos
                atributos={atributosEditables}
                valores={valores}
                onValor={(id, valor) => setValores((v) => ({ ...v, [id]: valor }))}
                onArchivo={() => {}}
              />
              {hayArchivos && (
                <p className="text-sm text-muted-foreground">
                  {tipo.nombre === vigente.tipoEspecie
                    ? 'Los archivos (grabaciones, fotos) se conservan de la versión vigente.'
                    : 'Al cambiar el tipo de especie, los archivos de la versión vigente no pasan a la nueva.'}
                </p>
              )}
            </div>
          )}

          <div className="flex flex-col gap-2">
            <Label htmlFor="r-justificacion">
              Justificación del cambio<span className="text-destructive" aria-hidden="true">*</span>
            </Label>
            <textarea id="r-justificacion" required rows={4} maxLength={JUSTIFICACION_MAXIMA} value={justificacion}
                      aria-describedby="r-justificacion-ayuda"
                      onChange={(e) => setJustificacion(e.target.value)}
                      placeholder="Ej.: Según la revisión de la familia Dendrobatidae (2026), la especie pasó al género Oophaga."
                      className={cn(claseCampo, 'py-2')} />
            <p id="r-justificacion-ayuda" className="text-xs text-muted-foreground">
              Queda guardada con tu nombre, la fecha y la hora, y no se puede editar ni borrar después.
              {' '}{largo}/{JUSTIFICACION_MAXIMA}
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <Button type="submit" disabled={enviando || !tipoId} className="h-11 px-5">
              <GitBranch aria-hidden="true" />
              {enviando ? 'Guardando…' : 'Guardar reclasificación'}
            </Button>
            <button type="button" onClick={onCancelar} disabled={enviando}
                    className={buttonVariants({ variant: 'outline', className: 'h-11 px-5' })}>
              Cancelar
            </button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

// Los valores de la versión vigente, con los ids de las definiciones del tipo (sin archivos)
function valoresDe(tipo: TipoEspecie, version: VersionFicha): Record<string, string> {
  const valores: Record<string, string> = {};
  for (const definicion of tipo.atributos) {
    const actual = version.atributos.find((a) => a.nombre === definicion.nombre);
    if (actual && definicion.tipoDato !== 'ARCHIVO') valores[definicion.id] = actual.valor;
  }
  return valores;
}
