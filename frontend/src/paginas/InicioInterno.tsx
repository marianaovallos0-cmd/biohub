import { useSesion } from '@/auth/SesionContext';

export default function InicioInterno() {
  const { usuario } = useSesion();
  return (
    <section className="flex flex-col gap-2">
      <h1 className="text-3xl font-extrabold tracking-tight text-bosque">
        Hola, {usuario?.nombre.split(' ')[0]}
      </h1>
      <p className="text-muted-foreground">Usa el menú de la izquierda para ir a las funciones de tu rol.</p>
    </section>
  );
}
