import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { FolderOpen, History, Search } from 'lucide-react';
import { listarHallazgosAuditoria, type ResumenHallazgo } from '@/api/trazabilidad';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { buttonVariants } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

const formatoFecha = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });

// HU-06, criterio 3: el auditor ve la clasificación vigente de cada hallazgo y si tiene versiones anteriores
export default function HallazgosAuditoria() {
  const [hallazgos, setHallazgos] = useState<ResumenHallazgo[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [filtro, setFiltro] = useState('');

  useEffect(() => {
    listarHallazgosAuditoria()
      .then(setHallazgos)
      .catch(() => setError('No pudimos cargar los hallazgos. Intenta de nuevo en un momento.'));
  }, []);

  const visibles = useMemo(() => {
    const texto = filtro.trim().toLowerCase();
    if (!hallazgos || !texto) return hallazgos;
    return hallazgos.filter((h) =>
      [h.especie, h.familia, h.departamento, h.expedicion, h.investigador]
        .some((campo) => campo?.toLowerCase().includes(texto)));
  }, [hallazgos, filtro]);

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-2">
        <h1 className="text-3xl font-extrabold tracking-tight text-bosque">Hallazgos</h1>
        <p className="text-muted-foreground">
          La clasificación vigente de cada hallazgo. Abre uno para ver su historial completo o reclasificarlo.
        </p>
      </section>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {!hallazgos && !error && <p className="text-sm text-muted-foreground">Cargando…</p>}

      {hallazgos?.length === 0 && (
        <Empty className="rounded-3xl border bg-card">
          <EmptyHeader>
            <EmptyMedia variant="icon"><FolderOpen /></EmptyMedia>
            <EmptyTitle>Todavía no hay hallazgos registrados</EmptyTitle>
            <EmptyDescription>Cuando un investigador registre uno, aparece aquí.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      )}

      {hallazgos && hallazgos.length > 0 && (
        <Card className="rounded-3xl">
          <CardHeader>
            <CardTitle className="text-xl font-extrabold text-bosque">Clasificación vigente</CardTitle>
            <CardDescription>
              {hallazgos.length} {hallazgos.length === 1 ? 'hallazgo' : 'hallazgos'} ·{' '}
              {hallazgos.filter((h) => h.versionVigente > 1).length} reclasificados
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div className="flex max-w-sm flex-col gap-2">
              <Label htmlFor="filtro">Filtrar</Label>
              <div className="relative">
                <Search aria-hidden="true" className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input id="filtro" type="search" value={filtro} onChange={(e) => setFiltro(e.target.value)}
                       placeholder="Especie, departamento, investigador…" className="h-11 pl-9" />
              </div>
            </div>

            {visibles?.length === 0 ? (
              <p className="text-sm text-muted-foreground">Ningún hallazgo coincide con “{filtro.trim()}”.</p>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Especie vigente</TableHead>
                    <TableHead>Tipo</TableHead>
                    <TableHead>Departamento</TableHead>
                    <TableHead>Investigador</TableHead>
                    <TableHead>Registrado</TableHead>
                    <TableHead>Versiones</TableHead>
                    <TableHead><span className="sr-only">Acciones</span></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {visibles?.map((h) => (
                    <TableRow key={h.id}>
                      <TableCell>
                        <p className="font-semibold italic text-bosque">{h.especie}</p>
                        {h.familia && <p className="text-xs text-muted-foreground">{h.familia}</p>}
                      </TableCell>
                      <TableCell><Badge variant="secondary">{h.tipoEspecie}</Badge></TableCell>
                      <TableCell>{h.departamento ?? '—'}</TableCell>
                      <TableCell>{h.investigador}</TableCell>
                      <TableCell className="whitespace-nowrap">{formatoFecha.format(new Date(h.fechaRegistro))}</TableCell>
                      <TableCell>
                        {h.versionVigente > 1 ? (
                          <Badge className="bg-ambar text-ambar-foreground">v{h.versionVigente} · tiene anteriores</Badge>
                        ) : (
                          <span className="text-sm text-muted-foreground">v1 · original</span>
                        )}
                      </TableCell>
                      <TableCell className="text-right">
                        <Link to={`/interno/trazabilidad/${h.id}`}
                              className={buttonVariants({ variant: 'outline', className: 'h-10 px-3' })}>
                          <History aria-hidden="true" />
                          Historial
                        </Link>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
