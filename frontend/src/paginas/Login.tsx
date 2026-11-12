import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { Leaf, LogIn, ShieldCheck } from 'lucide-react';
import { useSesion } from '@/auth/SesionContext';
import { Logo } from '@/components/biohub/Logo';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';

// HU-12
export default function Login() {
  const { usuario, iniciar } = useSesion();
  const navegar = useNavigate();
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  if (usuario) return <Navigate to="/interno" replace />;

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setEnviando(true);
    try {
      await iniciar(correo.trim(), contrasena);
      navegar('/interno', { replace: true });
    } catch (err) {
      setError(err instanceof Error && err.message.includes('401')
        ? 'Correo o contraseña incorrectos.'
        : 'No pudimos iniciar sesión. Intenta de nuevo en un momento.');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="flex min-h-[calc(100vh-180px)] items-center justify-center py-6">
      <div className="grid w-full max-w-4xl overflow-hidden rounded-3xl border bg-card shadow-sm md:grid-cols-2">
        <section className="relative flex flex-col justify-between gap-10 overflow-hidden bg-bosque p-8 text-white md:p-10">
          <Leaf aria-hidden="true" className="absolute -right-10 -top-10 size-48 rotate-12 text-brote/10" />
          <Leaf aria-hidden="true" className="absolute -bottom-12 -left-8 size-40 -rotate-45 text-brote/10" />

          <Logo />

          <div className="relative flex flex-col gap-3">
            <h1 className="text-3xl font-extrabold leading-tight tracking-tight">
              Registro y vigilancia de la biodiversidad
            </h1>
            <p className="text-white/75">
              Zona interna para el personal del Instituto. Cada quien ve solo lo que su rol le permite.
            </p>
          </div>

          <p className="relative flex items-center gap-2 text-sm text-brote">
            <ShieldCheck aria-hidden="true" className="size-4" />
            Las ubicaciones exactas solo las ve el personal autorizado.
          </p>
        </section>

        <section className="flex flex-col justify-center gap-6 p-8 md:p-10">
          <div className="flex flex-col gap-1">
            <h2 className="text-2xl font-extrabold tracking-tight text-bosque">Ingresar</h2>
            <p className="text-sm text-muted-foreground">Usa el correo y la contraseña que te dio el administrador.</p>
          </div>

          <form onSubmit={enviar} className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="correo">Correo</Label>
              <Input
                id="correo"
                type="email"
                autoComplete="username"
                placeholder="tucorreo@ejemplo.com"
                required
                value={correo}
                onChange={(e) => setCorreo(e.target.value)}
                className="h-11"
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="contrasena">Contraseña</Label>
              <Input
                id="contrasena"
                type="password"
                autoComplete="current-password"
                required
                value={contrasena}
                onChange={(e) => setContrasena(e.target.value)}
                className="h-11"
              />
            </div>

            {error && (
              <Alert variant="destructive">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <Button type="submit" disabled={enviando} className="mt-2 h-11">
              <LogIn aria-hidden="true" />
              {enviando ? 'Ingresando…' : 'Ingresar'}
            </Button>
          </form>

          <p className="text-center text-xs text-muted-foreground">
            ¿No tienes cuenta? Pídela al administrador del sistema.
          </p>
        </section>
      </div>
    </div>
  );
}
