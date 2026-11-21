import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { CircleCheck, Plus } from 'lucide-react';
import { ErrorApi } from '@/api/cliente';
import {
  agregarAtributo,
  listarTiposEspecie,
  nombreLegible,
  NOMBRE_TIPO_DATO,
  type TipoDato,
  type TipoEspecie,
} from '@/api/taxonomia';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

const claseSelect =
  'h-11 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50';

/**
 * HU-03. El investigador agrega un atributo a un tipo de especie con el sistema corriendo.
 * Es un dato nuevo, no código nuevo: aparece en "Registrar hallazgo" sin recompilar ni reiniciar (ASR-7).
 */
export default function TiposEspecie() {
  const [tipos, setTipos] = useState<TipoEspecie[] | null>(null);
  const [errorCarga, setErrorCarga] = useState<string | null>(null);

  const [tipoId, setTipoId] = useState('');
  const [nombre, setNombre] = useState('');
  const [tipoDato, setTipoDato] = useState<TipoDato>('TEXTO');
  const [obligatorio, setObligatorio] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [agregado, setAgregado] = useState<{ atributo: string; tipo: string } | null>(null);

  function cargar() {
    return listarTiposEspecie()
      .then(setTipos)
      .catch(() => setErrorCarga('No pudimos cargar los tipos de especie. Intenta de nuevo en un momento.'));
  }

  useEffect(() => {
    cargar();
  }, []);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setEnviando(true);
    setError(null);
    setAgregado(null);
    try {
      const creado = await agregarAtributo(tipoId, { nombre, tipoDato, obligatorio });
      const tipo = tipos?.find((t) => t.id === tipoId)?.nombre ?? '';
      setAgregado({ atributo: nombreLegible(creado.nombre), tipo });
      setNombre('');
      setObligatorio(false);
      await cargar();
    } catch (err) {
      if (err instanceof ErrorApi && err.detalle) setError(err.detalle);
      else if (err instanceof ErrorApi && err.estado === 403) setError('Tu rol no tiene permiso para agregar atributos.');
      else setError('No pudimos agregar el atributo. Intenta de nuevo en un momento.');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-2">
        <h1 className="text-3xl font-extrabold tracking-tight text-bosque">Tipos de especie</h1>
        <p className="text-muted-foreground">
          Cada tipo de especie tiene sus propios atributos. Si cambia lo que se mide, agrega un atributo aquí y queda
          disponible de inmediato en el registro de hallazgos.
        </p>
      </section>

      {errorCarga && (
        <Alert variant="destructive">
          <AlertDescription>{errorCarga}</AlertDescription>
        </Alert>
      )}

      <Card className="rounded-3xl">
        <CardHeader>
          <CardTitle className="text-xl font-extrabold text-bosque">Agregar un atributo</CardTitle>
          <CardDescription>
            Los hallazgos ya registrados no cambian. Si el atributo es obligatorio, solo se pide en los registros nuevos.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={enviar} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-[1fr_1fr_180px_auto] lg:items-end">
            <div className="flex flex-col gap-2">
              <Label htmlFor="tipo">Tipo de especie<span className="text-destructive" aria-hidden="true">*</span></Label>
              <select id="tipo" required value={tipoId} disabled={!tipos} onChange={(e) => setTipoId(e.target.value)}
                      className={claseSelect}>
                <option value="">{tipos ? 'Elige un tipo' : 'Cargando…'}</option>
                {tipos?.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
              </select>
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="nombre">Nombre del atributo<span className="text-destructive" aria-hidden="true">*</span></Label>
              <Input id="nombre" required maxLength={100} value={nombre} placeholder="Ej.: Color de piel"
                     onChange={(e) => setNombre(e.target.value)} className="h-11" />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="tipo-dato">Tipo de dato<span className="text-destructive" aria-hidden="true">*</span></Label>
              <select id="tipo-dato" value={tipoDato} onChange={(e) => setTipoDato(e.target.value as TipoDato)}
                      className={claseSelect}>
                {(Object.keys(NOMBRE_TIPO_DATO) as TipoDato[]).map((t) => (
                  <option key={t} value={t}>{NOMBRE_TIPO_DATO[t]}</option>
                ))}
              </select>
            </div>
            <Button type="submit" disabled={enviando || !tipoId} className="h-11 px-6">
              <Plus aria-hidden="true" />
              {enviando ? 'Agregando…' : 'Agregar'}
            </Button>
            <label className="flex items-center gap-3 text-sm sm:col-span-2 lg:col-span-4">
              <input type="checkbox" checked={obligatorio} onChange={(e) => setObligatorio(e.target.checked)}
                     className="size-5 accent-bosque" />
              Obligatorio en los registros nuevos
            </label>
          </form>

          <div aria-live="polite" className="mt-4 flex flex-col gap-3">
            {error && (
              <Alert variant="destructive">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            {agregado && (
              <Alert>
                <CircleCheck aria-hidden="true" />
                <AlertDescription>
                  «{agregado.atributo}» quedó agregado a {agregado.tipo}, sin reiniciar el sistema.{' '}
                  <Link to="/interno/hallazgos/nuevo" className="font-semibold text-bosque underline">
                    Verlo en Registrar hallazgo
                  </Link>
                </AlertDescription>
              </Alert>
            )}
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-6 lg:grid-cols-2">
        {tipos?.map((tipo) => (
          <Card key={tipo.id} className="rounded-3xl">
            <CardHeader>
              <CardTitle className="text-xl font-extrabold text-bosque">{tipo.nombre}</CardTitle>
              <CardDescription>
                {tipo.atributos.length} {tipo.atributos.length === 1 ? 'atributo' : 'atributos'}
              </CardDescription>
            </CardHeader>
            <CardContent>
              {tipo.atributos.length === 0 ? (
                <p className="text-sm text-muted-foreground">Todavía no tiene atributos propios.</p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Atributo</TableHead>
                      <TableHead>Tipo de dato</TableHead>
                      <TableHead>Obligatorio</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {tipo.atributos.map((a) => (
                      <TableRow key={a.id}>
                        <TableCell className="font-semibold">{nombreLegible(a.nombre)}</TableCell>
                        <TableCell>{NOMBRE_TIPO_DATO[a.tipoDato]}</TableCell>
                        <TableCell>
                          {a.obligatorio ? <Badge>Sí</Badge> : <Badge variant="secondary">No</Badge>}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
