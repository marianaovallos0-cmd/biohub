import { useEffect, useState, type FormEvent } from 'react';
import { CircleCheck, ExternalLink, Leaf, Paperclip, Plus, Trash2 } from 'lucide-react';
import { ErrorApi } from '@/api/cliente';
import { listarTiposEspecie, type TipoEspecie } from '@/api/taxonomia';
import {
  abrirEvidencia,
  DEPARTAMENTOS,
  FORMATOS_EVIDENCIA,
  MAXIMO_BYTES,
  NOMBRE_EVIDENCIA,
  registrarHallazgo,
  type HallazgoRegistrado,
  type TipoEvidencia,
} from '@/api/hallazgos';
import { CamposAtributos } from '@/components/biohub/CamposAtributos';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

// Los rangos taxonómicos en orden. Género y especie son obligatorios (nombre científico).
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

interface EvidenciaFormulario {
  clave: number;
  tipo: TipoEvidencia;
  descripcion: string;
  archivo: File | null;
}

const claseSelect =
  'h-11 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50';

const rangosVacios = (): Record<Rango, string> =>
  ({ reino: 'Animalia', filo: '', clase: '', orden: '', familia: '', genero: '', especie: '' });

let siguienteClave = 1;

export default function RegistrarHallazgo() {
  const [tipos, setTipos] = useState<TipoEspecie[] | null>(null);
  const [errorCarga, setErrorCarga] = useState<string | null>(null);

  const [tipoId, setTipoId] = useState('');
  const [expedicion, setExpedicion] = useState('');
  const [rangos, setRangos] = useState(rangosVacios);
  const [valores, setValores] = useState<Record<string, string>>({});
  const [archivosAtributos, setArchivosAtributos] = useState<Record<string, File>>({});
  const [latitud, setLatitud] = useState('');
  const [longitud, setLongitud] = useState('');
  const [departamento, setDepartamento] = useState('');
  const [protegida, setProtegida] = useState(false);
  const [evidencias, setEvidencias] = useState<EvidenciaFormulario[]>([]);

  const [enviando, setEnviando] = useState(false);
  const [errores, setErrores] = useState<string[]>([]);
  const [registrado, setRegistrado] = useState<HallazgoRegistrado | null>(null);
  const [errorEvidencia, setErrorEvidencia] = useState<string | null>(null);

  useEffect(() => {
    listarTiposEspecie()
      .then(setTipos)
      .catch(() => setErrorCarga('No pudimos cargar los tipos de especie. Intenta de nuevo en un momento.'));
  }, []);

  const tipo = tipos?.find((t) => t.id === tipoId) ?? null;

  // HU-02: al cambiar de tipo, la ficha muestra solo los atributos de ese tipo y se olvidan los del anterior
  function elegirTipo(id: string) {
    setTipoId(id);
    setValores({});
    setArchivosAtributos({});
  }

  function cambiarArchivoAtributo(id: string, archivo: File | null) {
    setArchivosAtributos((actuales) => {
      const nuevos = { ...actuales };
      if (archivo) nuevos[id] = archivo;
      else delete nuevos[id];
      return nuevos;
    });
  }

  function cambiarEvidencia(clave: number, cambios: Partial<EvidenciaFormulario>) {
    setEvidencias((actuales) => actuales.map((e) => (e.clave === clave ? { ...e, ...cambios } : e)));
  }

  function agregarEvidencia() {
    setEvidencias((actuales) => [...actuales, { clave: siguienteClave++, tipo: 'FOTOGRAFIA', descripcion: '', archivo: null }]);
  }

  function reiniciar() {
    setTipoId('');
    setExpedicion('');
    setRangos(rangosVacios());
    setValores({});
    setArchivosAtributos({});
    setLatitud('');
    setLongitud('');
    setDepartamento('');
    setProtegida(false);
    setEvidencias([]);
    setErrores([]);
    setRegistrado(null);
    setErrorEvidencia(null);
  }

  async function verEvidencia(id: string) {
    setErrorEvidencia(null);
    try {
      await abrirEvidencia(id);
    } catch {
      setErrorEvidencia('No pudimos abrir la evidencia. Intenta de nuevo en un momento.');
    }
  }

  // Lo que se puede revisar sin ir al servidor; el backend vuelve a validar todo
  function revisarArchivos(): string[] {
    const problemas: string[] = [];
    evidencias.forEach((e, i) => {
      if (!e.archivo) problemas.push(`Evidencia ${i + 1}: falta el archivo.`);
      else if (e.archivo.size > MAXIMO_BYTES) problemas.push(`Evidencia ${i + 1} (${e.archivo.name}): pesa más de 10 MB.`);
    });
    Object.values(archivosAtributos).forEach((archivo) => {
      if (archivo.size > MAXIMO_BYTES) problemas.push(`Archivo ${archivo.name}: pesa más de 10 MB.`);
    });
    return problemas;
  }

  async function enviar(e: FormEvent) {
    e.preventDefault();
    const problemas = revisarArchivos();
    if (problemas.length > 0) {
      setErrores(problemas);
      return;
    }

    setEnviando(true);
    setErrores([]);
    try {
      const resultado = await registrarHallazgo(
        {
          expedicion,
          ficha: { tipoEspecieId: tipoId, ...rangos, valores },
          ubicacion: {
            latitud: latitud === '' ? null : Number(latitud),
            longitud: longitud === '' ? null : Number(longitud),
            region: DEPARTAMENTOS[departamento] ?? '',
            departamento,
            especieProtegida: protegida,
          },
          evidencias: evidencias.map(({ tipo, descripcion }) => ({ tipo, descripcion })),
        },
        evidencias.map((ev) => ev.archivo as File),
        archivosAtributos,
      );
      setRegistrado(resultado);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (error) {
      if (error instanceof ErrorApi && error.errores.length > 0) setErrores(error.errores);
      else if (error instanceof ErrorApi && error.estado === 413) setErrores(['Los archivos pesan demasiado. Cada uno puede pesar máximo 10 MB.']);
      else if (error instanceof ErrorApi && error.estado === 403) setErrores(['Tu rol no tiene permiso para registrar hallazgos.']);
      else setErrores(['No pudimos guardar el hallazgo. Revisa tu conexión e intenta de nuevo.']);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } finally {
      setEnviando(false);
    }
  }

  if (registrado) {
    return (
      <div className="flex flex-col gap-6">
        <Card className="rounded-3xl">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-2xl font-extrabold text-bosque">
              <CircleCheck aria-hidden="true" className="size-7 text-exito" />
              Hallazgo registrado
            </CardTitle>
            <CardDescription aria-live="polite">
              <span className="italic">{registrado.especie}</span> quedó guardado en BioHub a nombre de {registrado.autor}.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <dl className="grid gap-3 text-sm sm:grid-cols-3">
              <div>
                <dt className="text-muted-foreground">Fecha y hora</dt>
                <dd className="font-semibold">
                  {new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(registrado.fechaRegistro))}
                </dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Evidencias</dt>
                <dd className="font-semibold">{registrado.evidencias.length}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Código</dt>
                <dd className="font-mono text-xs">{registrado.id}</dd>
              </div>
            </dl>
            {registrado.evidencias.length > 0 && (
              <div className="flex flex-col gap-2">
                <h2 className="font-bold text-bosque">Evidencias guardadas</h2>
                <ul className="flex flex-col gap-2">
                  {registrado.evidencias.map((ev) => (
                    <li key={ev.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border p-3">
                      <div className="min-w-0">
                        <p className="font-semibold">{ev.nombreOriginal}</p>
                        <p className="text-sm text-muted-foreground">
                          {NOMBRE_EVIDENCIA[ev.tipo]}{ev.descripcion ? ` · ${ev.descripcion}` : ''}
                        </p>
                      </div>
                      <Button type="button" variant="outline" className="h-11 px-4" onClick={() => verEvidencia(ev.id)}>
                        <ExternalLink aria-hidden="true" />
                        Ver
                      </Button>
                    </li>
                  ))}
                </ul>
                {errorEvidencia && (
                  <Alert variant="destructive">
                    <AlertDescription>{errorEvidencia}</AlertDescription>
                  </Alert>
                )}
              </div>
            )}
            <Button type="button" onClick={reiniciar} className="h-11 w-fit px-6">
              <Plus aria-hidden="true" />
              Registrar otro hallazgo
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <form onSubmit={enviar} className="flex flex-col gap-6">
      <section className="flex flex-col gap-2">
        <h1 className="text-3xl font-extrabold tracking-tight text-bosque">Registrar hallazgo</h1>
        <p className="text-muted-foreground">
          Lo que registres queda guardado a tu nombre, con la fecha y la hora. Los campos con <span className="text-destructive">*</span> son obligatorios.
        </p>
      </section>

      {errorCarga && (
        <Alert variant="destructive">
          <AlertDescription>{errorCarga}</AlertDescription>
        </Alert>
      )}

      {errores.length > 0 && (
        <Alert variant="destructive" aria-live="assertive">
          <AlertTitle>El hallazgo no se guardó. Corrige esto:</AlertTitle>
          <AlertDescription>
            <ul className="list-disc pl-5">
              {errores.map((error) => <li key={error}>{error}</li>)}
            </ul>
          </AlertDescription>
        </Alert>
      )}

      {/* 1. Tipo de especie y expedición */}
      <Card className="rounded-3xl">
        <CardHeader>
          <CardTitle className="text-xl font-extrabold text-bosque">1. Especie</CardTitle>
          <CardDescription>El tipo de especie decide qué atributos se piden en la ficha.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-2">
            <Label htmlFor="tipo">Tipo de especie<span className="text-destructive" aria-hidden="true">*</span></Label>
            <select id="tipo" required value={tipoId} onChange={(e) => elegirTipo(e.target.value)}
                    disabled={!tipos} className={claseSelect}>
              <option value="">{tipos ? 'Elige un tipo' : 'Cargando…'}</option>
              {tipos?.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
            </select>
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="expedicion">Expedición</Label>
            <Input id="expedicion" maxLength={150} value={expedicion} placeholder="Ej.: Expedición Atrato 2026"
                   onChange={(e) => setExpedicion(e.target.value)} className="h-11" />
          </div>
        </CardContent>
      </Card>

      {/* 2. Clasificación y atributos del tipo (HU-02) */}
      {tipo && (
        <Card className="rounded-3xl">
          <CardHeader>
            <CardTitle className="text-xl font-extrabold text-bosque">2. Ficha taxonómica</CardTitle>
            <CardDescription>Clasificación y atributos propios de: {tipo.nombre}.</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-6">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {RANGOS.map(({ campo, texto, obligatorio }) => (
                <div key={campo} className="flex flex-col gap-2">
                  <Label htmlFor={campo}>
                    {texto}
                    {obligatorio && <span className="text-destructive" aria-hidden="true">*</span>}
                  </Label>
                  <Input id={campo} required={obligatorio} maxLength={100} value={rangos[campo]}
                         className={campo === 'genero' || campo === 'especie' ? 'h-11 italic' : 'h-11'}
                         onChange={(e) => setRangos((r) => ({ ...r, [campo]: e.target.value }))} />
                </div>
              ))}
            </div>
            <div className="flex flex-col gap-3">
              <h2 className="font-bold text-bosque">Atributos de {tipo.nombre.toLowerCase()}</h2>
              <CamposAtributos
                atributos={tipo.atributos}
                valores={valores}
                onValor={(id, valor) => setValores((v) => ({ ...v, [id]: valor }))}
                onArchivo={cambiarArchivoAtributo}
              />
            </div>
          </CardContent>
        </Card>
      )}

      {/* 3. Ubicación */}
      <Card className="rounded-3xl">
        <CardHeader>
          <CardTitle className="text-xl font-extrabold text-bosque">3. Ubicación</CardTitle>
          <CardDescription>
            Las coordenadas exactas solo las ve el personal de alto rango. El público solo ve la región y el departamento.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-2">
            <Label htmlFor="latitud">Latitud<span className="text-destructive" aria-hidden="true">*</span></Label>
            <Input id="latitud" type="number" step="any" min={-90} max={90} required inputMode="decimal"
                   placeholder="Ej.: 5.6947" value={latitud} onChange={(e) => setLatitud(e.target.value)} className="h-11" />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="longitud">Longitud<span className="text-destructive" aria-hidden="true">*</span></Label>
            <Input id="longitud" type="number" step="any" min={-180} max={180} required inputMode="decimal"
                   placeholder="Ej.: -76.6611" value={longitud} onChange={(e) => setLongitud(e.target.value)} className="h-11" />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="departamento">Departamento<span className="text-destructive" aria-hidden="true">*</span></Label>
            <select id="departamento" required value={departamento} onChange={(e) => setDepartamento(e.target.value)}
                    className={claseSelect}>
              <option value="">Elige un departamento</option>
              {Object.keys(DEPARTAMENTOS).map((d) => <option key={d} value={d}>{d}</option>)}
            </select>
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="region">Región</Label>
            <Input id="region" readOnly value={DEPARTAMENTOS[departamento] ?? ''} placeholder="Sale del departamento"
                   className="h-11 bg-muted" />
          </div>
          <label className="flex items-center gap-3 text-sm sm:col-span-2">
            <input type="checkbox" checked={protegida} onChange={(e) => setProtegida(e.target.checked)}
                   className="size-5 accent-bosque" />
            Es una especie protegida o en peligro
          </label>
        </CardContent>
      </Card>

      {/* 4. Evidencias */}
      <Card className="rounded-3xl">
        <CardHeader>
          <CardTitle className="text-xl font-extrabold text-bosque">4. Evidencias</CardTitle>
          <CardDescription>Fotos, muestras, notas de campo o audios. Máximo 10 MB cada archivo.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {evidencias.length === 0 && (
            <p className="text-sm text-muted-foreground">Todavía no has adjuntado evidencias. Son opcionales.</p>
          )}
          {evidencias.map((ev, i) => (
            <fieldset key={ev.clave} className="grid gap-3 rounded-2xl border p-4 sm:grid-cols-[180px_1fr_1fr_auto] sm:items-end">
              <legend className="sr-only">Evidencia {i + 1}</legend>
              <div className="flex flex-col gap-2">
                <Label htmlFor={`ev-tipo-${ev.clave}`}>Tipo</Label>
                <select id={`ev-tipo-${ev.clave}`} value={ev.tipo} className={claseSelect}
                        onChange={(e) => cambiarEvidencia(ev.clave, { tipo: e.target.value as TipoEvidencia, archivo: null })}>
                  {(Object.keys(NOMBRE_EVIDENCIA) as TipoEvidencia[]).map((t) => (
                    <option key={t} value={t}>{NOMBRE_EVIDENCIA[t]}</option>
                  ))}
                </select>
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor={`ev-archivo-${ev.clave}`}>Archivo</Label>
                <Input key={`${ev.clave}-${ev.tipo}`} id={`ev-archivo-${ev.clave}`} type="file" required
                       accept={FORMATOS_EVIDENCIA[ev.tipo]} className="h-11"
                       onChange={(e) => cambiarEvidencia(ev.clave, { archivo: e.target.files?.[0] ?? null })} />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor={`ev-desc-${ev.clave}`}>Descripción</Label>
                <Input id={`ev-desc-${ev.clave}`} maxLength={255} value={ev.descripcion} className="h-11"
                       onChange={(e) => cambiarEvidencia(ev.clave, { descripcion: e.target.value })} />
              </div>
              <Button type="button" variant="ghost" className="h-11"
                      aria-label={`Quitar la evidencia ${i + 1}`}
                      onClick={() => setEvidencias((actuales) => actuales.filter((e) => e.clave !== ev.clave))}>
                <Trash2 aria-hidden="true" />
              </Button>
            </fieldset>
          ))}
          <Button type="button" variant="outline" onClick={agregarEvidencia} className="h-11 w-fit px-5">
            <Paperclip aria-hidden="true" />
            Adjuntar evidencia
          </Button>
        </CardContent>
      </Card>

      <Button type="submit" disabled={enviando || !tipo} className="h-12 w-full px-8 text-base sm:w-fit">
        <Leaf aria-hidden="true" />
        {enviando ? 'Guardando…' : 'Registrar hallazgo'}
      </Button>
    </form>
  );
}
