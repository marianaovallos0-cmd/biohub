import { pedir } from '@/api/cliente';

// Lo que devuelve GET /interno/tipos-especie (HU-02)
export type TipoDato = 'NUMERO' | 'TEXTO' | 'FECHA' | 'ARCHIVO';

export interface AtributoDefinido {
  id: string;
  nombre: string;
  tipoDato: TipoDato;
  obligatorio: boolean;
}

export interface TipoEspecie {
  id: string;
  nombre: string;
  atributos: AtributoDefinido[];
}

export function listarTiposEspecie() {
  return pedir<TipoEspecie[]>('/interno/tipos-especie');
}

// "temperatura_agua_c" -> "Temperatura agua c"
export function nombreLegible(nombre: string) {
  const texto = nombre.replaceAll('_', ' ');
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

// Los mismos formatos que acepta el backend (ValidadorArchivo)
export const ARCHIVOS_PERMITIDOS = '.jpg,.jpeg,.png,.pdf,.mp3,.wav';

// HU-03: POST /interno/tipos-especie/{id}/atributos. No se reinicia nada: el formulario lo trae en la siguiente carga
export function agregarAtributo(tipoId: string, atributo: { nombre: string; tipoDato: TipoDato; obligatorio: boolean }) {
  return pedir<AtributoDefinido>(`/interno/tipos-especie/${tipoId}/atributos`, {
    method: 'POST',
    body: JSON.stringify(atributo),
  });
}

export const NOMBRE_TIPO_DATO: Record<TipoDato, string> = {
  NUMERO: 'Número',
  TEXTO: 'Texto',
  FECHA: 'Fecha',
  ARCHIVO: 'Archivo',
};
