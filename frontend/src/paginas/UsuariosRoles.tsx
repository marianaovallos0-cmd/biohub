import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Check, Plus, UserPlus, UserRoundCheck, UserRoundX } from 'lucide-react';
import { pedir } from '@/api/cliente';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

// Lo que devuelven GET /interno/usuarios, /interno/roles y /interno/permisos (HU-15)
interface UsuarioGestion {
  id: string;
  nombre: string;
  correo: string;
  activo: boolean;
  rolId: string;
  rol: string;
  nivel: string;
}

interface RolGestion {
  id: string;
  nombre: string;
  nivel: string;
  permisos: string[];
}

interface PermisoCatalogo {
  codigo: string;
  descripcion: string | null;
}

interface Datos {
  usuarios: UsuarioGestion[];
  roles: RolGestion[];
  permisos: PermisoCatalogo[];
}

const claseSelect =
  'h-11 w-full rounded-lg border border-input bg-transparent px-3 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50';

// El backend responde problem+json con "detail"; cliente.ts solo lanza el código, así que aquí se lee el detalle
async function enviar<T>(ruta: string, metodo: string, cuerpo?: unknown): Promise<T> {
  const token = sessionStorage.getItem('biohub_token');
  const respuesta = await fetch(ruta, {
    method: metodo,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  });
  if (!respuesta.ok) {
    const problema = await respuesta.json().catch(() => null);
    throw new Error(problema?.detail ?? `No se pudo completar la acción (error ${respuesta.status}).`);
  }
  return respuesta.json() as Promise<T>;
}

export default function UsuariosRoles() {
  const [datos, setDatos] = useState<Datos | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);

  // Formulario de usuario nuevo
  const [nombre, setNombre] = useState('');
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [rolId, setRolId] = useState('');

  const cargar = useCallback(() => {
    return Promise.all([
      pedir<UsuarioGestion[]>('/interno/usuarios'),
      pedir<RolGestion[]>('/interno/roles'),
      pedir<PermisoCatalogo[]>('/interno/permisos'),
    ]).then(([usuarios, roles, permisos]) => setDatos({ usuarios, roles, permisos }));
  }, []);

  useEffect(() => {
    cargar().catch(() => setError('No pudimos cargar los usuarios y roles. Intenta de nuevo en un momento.'));
  }, [cargar]);

  async function accion(hacer: () => Promise<unknown>, exito: string) {
    setOcupado(true);
    setError(null);
    setAviso(null);
    try {
      await hacer();
      await cargar();
      setAviso(exito);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setOcupado(false);
    }
  }

  function crear(e: FormEvent) {
    e.preventDefault();
    // Tercer criterio: sin rol no se guarda (el backend también lo valida)
    if (!rolId) {
      setError('El usuario debe tener un rol.');
      return;
    }
    void accion(async () => {
      await enviar('/interno/usuarios', 'POST', { nombre, correo, contrasena, rolId });
      setNombre('');
      setCorreo('');
      setContrasena('');
      setRolId('');
    }, `Se creó el usuario ${correo.trim().toLowerCase()}.`);
  }

  const descripcionPermiso = (codigo: string) =>
    datos?.permisos.find((p) => p.codigo === codigo)?.descripcion ?? codigo;

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-2">
        <h1 className="text-3xl font-extrabold tracking-tight text-bosque">Usuarios y roles</h1>
        <p className="text-muted-foreground">
          Crea usuarios, asígnales un rol y define qué puede hacer cada rol. Los cambios de un rol aplican de
          inmediato a todos sus usuarios.
        </p>
      </section>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      {aviso && !error && (
        <Alert>
          <AlertDescription aria-live="polite">{aviso}</AlertDescription>
        </Alert>
      )}

      {!datos && !error && <p className="text-muted-foreground" aria-live="polite">Cargando…</p>}

      {datos && (
        <>
          <Card className="rounded-3xl">
            <CardHeader>
              <CardTitle className="text-xl font-extrabold text-bosque">Nuevo usuario</CardTitle>
              <CardDescription>Queda activo con los permisos del rol que elijas.</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={crear} className="grid gap-4 md:grid-cols-2 lg:grid-cols-4 lg:items-end">
                <div className="flex flex-col gap-2">
                  <Label htmlFor="nombre">Nombre</Label>
                  <Input id="nombre" required maxLength={100} value={nombre}
                         onChange={(e) => setNombre(e.target.value)} className="h-11" />
                </div>
                <div className="flex flex-col gap-2">
                  <Label htmlFor="correo">Correo</Label>
                  <Input id="correo" type="email" required maxLength={150} value={correo}
                         onChange={(e) => setCorreo(e.target.value)} className="h-11" />
                </div>
                <div className="flex flex-col gap-2">
                  <Label htmlFor="contrasena">Contraseña inicial</Label>
                  <Input id="contrasena" type="password" required minLength={8} autoComplete="new-password"
                         value={contrasena} onChange={(e) => setContrasena(e.target.value)} className="h-11" />
                </div>
                <div className="flex flex-col gap-2">
                  <Label htmlFor="rol">Rol</Label>
                  <select id="rol" value={rolId} onChange={(e) => setRolId(e.target.value)} className={claseSelect}>
                    <option value="">Elige un rol…</option>
                    {datos.roles.map((rol) => (
                      <option key={rol.id} value={rol.id}>{rol.nombre}</option>
                    ))}
                  </select>
                </div>
                <Button type="submit" disabled={ocupado} className="h-11 px-6 lg:col-start-4">
                  <UserPlus aria-hidden="true" />
                  Crear usuario
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card className="rounded-3xl">
            <CardHeader>
              <CardTitle className="text-xl font-extrabold text-bosque">Usuarios</CardTitle>
              <CardDescription>
                Un usuario no se borra: se desactiva, para que su historial siga apuntando a alguien.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Usuario</TableHead>
                    <TableHead>Rol</TableHead>
                    <TableHead>Estado</TableHead>
                    <TableHead className="text-right">Acción</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {datos.usuarios.map((u) => (
                      <TableRow key={u.id}>
                        <TableCell>
                          <p className="font-semibold text-bosque">{u.nombre}</p>
                          <p className="text-sm text-muted-foreground">{u.correo}</p>
                        </TableCell>
                        <TableCell>
                          <select
                            aria-label={`Rol de ${u.nombre}`}
                            value={u.rolId}
                            disabled={ocupado}
                            onChange={(e) => {
                              const nuevo = datos.roles.find((r) => r.id === e.target.value);
                              void accion(
                                () => enviar(`/interno/usuarios/${u.id}/rol`, 'PUT', { rolId: e.target.value }),
                                `${u.nombre} ahora es ${nuevo?.nombre ?? 'otro rol'}.`,
                              );
                            }}
                            className={`${claseSelect} h-9 min-w-48 text-sm`}
                          >
                            {datos.roles.map((rol) => (
                              <option key={rol.id} value={rol.id}>{rol.nombre}</option>
                            ))}
                          </select>
                        </TableCell>
                        <TableCell>
                          <Badge variant={u.activo ? 'secondary' : 'outline'}>{u.activo ? 'Activo' : 'Inactivo'}</Badge>
                        </TableCell>
                        <TableCell className="text-right">
                          {u.activo ? (
                            <Button variant="outline" size="sm" disabled={ocupado}
                                    onClick={() => void accion(
                                      () => enviar(`/interno/usuarios/${u.id}/desactivar`, 'POST'),
                                      `${u.nombre} quedó inactivo.`)}>
                              <UserRoundX aria-hidden="true" />
                              Desactivar
                            </Button>
                          ) : (
                            <Button variant="outline" size="sm" disabled={ocupado}
                                    onClick={() => void accion(
                                      () => enviar(`/interno/usuarios/${u.id}/activar`, 'POST'),
                                      `${u.nombre} quedó activo.`)}>
                              <UserRoundCheck aria-hidden="true" />
                              Activar
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <section className="flex flex-col gap-4">
            <h2 className="text-2xl font-extrabold text-bosque">Permisos por rol</h2>
            <div className="grid gap-4 lg:grid-cols-2">
              {datos.roles.map((rol) => (
                <Card key={rol.id} className="rounded-3xl">
                  <CardHeader>
                    <CardTitle className="text-lg font-extrabold text-bosque">{rol.nombre}</CardTitle>
                    <CardDescription>
                      {datos.usuarios.filter((u) => u.rolId === rol.id).length} usuario(s) con este rol
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="flex flex-wrap gap-2">
                    {datos.permisos.map(({ codigo }) => {
                      const tiene = rol.permisos.includes(codigo);
                      return (
                        <Button
                          key={codigo}
                          type="button"
                          size="sm"
                          variant={tiene ? 'default' : 'outline'}
                          aria-pressed={tiene}
                          title={descripcionPermiso(codigo)}
                          disabled={ocupado}
                          onClick={() => void accion(
                            () => enviar(`/interno/roles/${rol.id}/permisos/${codigo}`, tiene ? 'DELETE' : 'PUT'),
                            `${tiene ? 'Se quitó' : 'Se agregó'} ${codigo} al rol ${rol.nombre}.`,
                          )}
                        >
                          {tiene ? <Check aria-hidden="true" /> : <Plus aria-hidden="true" />}
                          {codigo}
                        </Button>
                      );
                    })}
                  </CardContent>
                </Card>
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  );
}
