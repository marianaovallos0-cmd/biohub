import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ARCHIVOS_PERMITIDOS, nombreLegible, type AtributoDefinido, type TipoDato } from '@/api/taxonomia';

interface Props {
  atributos: AtributoDefinido[];
  valores: Record<string, string>;
  onValor: (id: string, valor: string) => void;
  onArchivo: (id: string, archivo: File | null) => void;
}

const AYUDA: Record<TipoDato, string> = {
  NUMERO: 'Número. Use punto para los decimales.',
  TEXTO: 'Texto.',
  FECHA: 'Fecha.',
  ARCHIVO: 'Archivo jpg, png, pdf, mp3 o wav de máximo 10 MB.',
};

// HU-02: los campos de la ficha salen de las definiciones que manda la API.
// Si mañana el anfibio mide algo nuevo (HU-03), aparece aquí sin tocar el frontend.
export function CamposAtributos({ atributos, valores, onValor, onArchivo }: Props) {
  if (atributos.length === 0) {
    return <p className="text-sm text-muted-foreground">Este tipo de especie no tiene atributos propios.</p>;
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {atributos.map((atributo) => {
        const id = `atributo-${atributo.id}`;
        const ayuda = `${id}-ayuda`;
        return (
          <div key={atributo.id} className="flex flex-col gap-2">
            <Label htmlFor={id}>
              {nombreLegible(atributo.nombre)}
              {atributo.obligatorio && <span className="text-destructive" aria-hidden="true">*</span>}
            </Label>

            {atributo.tipoDato === 'ARCHIVO' ? (
              <Input
                id={id}
                type="file"
                accept={ARCHIVOS_PERMITIDOS}
                required={atributo.obligatorio}
                aria-describedby={ayuda}
                onChange={(e) => onArchivo(atributo.id, e.target.files?.[0] ?? null)}
                className="h-11"
              />
            ) : (
              <Input
                id={id}
                type={atributo.tipoDato === 'NUMERO' ? 'number' : atributo.tipoDato === 'FECHA' ? 'date' : 'text'}
                step={atributo.tipoDato === 'NUMERO' ? 'any' : undefined}
                inputMode={atributo.tipoDato === 'NUMERO' ? 'decimal' : undefined}
                required={atributo.obligatorio}
                aria-describedby={ayuda}
                value={valores[atributo.id] ?? ''}
                onChange={(e) => onValor(atributo.id, e.target.value)}
                className="h-11"
              />
            )}

            <p id={ayuda} className="text-xs text-muted-foreground">
              {AYUDA[atributo.tipoDato]} {atributo.obligatorio ? 'Obligatorio.' : 'Opcional.'}
            </p>
          </div>
        );
      })}
    </div>
  );
}
