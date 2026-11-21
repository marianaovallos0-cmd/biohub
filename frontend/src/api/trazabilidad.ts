import { pedir } from '@/api/cliente';

// HU-06, HU-07 y HU-08 (auditor científico)

// GET /interno/historial/hallazgos
export interface ResumenHallazgo {
  id: string;
  fechaRegistro: string;
  expedicion: string | null;
  investigador: string;
  tipoEspecie: string;
  especie: string;
  familia: string | null;
  departamento: string | null;
  versionVigente: number;
}

// GET /interno/historial/hallazgos/{id}
export interface AtributoVersion {
  nombre: string;
  tipoDato: string;
  valor: string;
}

export interface CambioVersion {
  fechaHora: string;
  justificacion: string;
  autorizadoPor: string;
  versionAnterior: number;
}

export interface VersionFicha {
  id: string;
  numeroVersion: number;
  vigente: boolean;
  fechaVersion: string;
  tipoEspecie: string;
  reino: string | null;
  filo: string | null;
  clase: string | null;
  orden: string | null;
  familia: string | null;
  genero: string;
  especie: string;
  atributos: AtributoVersion[];
  cambio: CambioVersion | null;
}

export interface HistorialHallazgo {
  hallazgoId: string;
  fechaRegistro: string;
  expedicion: string | null;
  investigador: string;
  versionVigente: number;
  versiones: VersionFicha[];
}

// POST /interno/hallazgos/{id}/reclasificaciones
export interface DatosReclasificacion {
  justificacion: string;
  ficha: {
    tipoEspecieId: string;
    reino: string;
    filo: string;
    clase: string;
    orden: string;
    familia: string;
    genero: string;
    especie: string;
    valores: Record<string, string>;
  };
}

export interface ReclasificacionRegistrada {
  hallazgoId: string;
  versionAnterior: number;
  versionNueva: number;
  especieAnterior: string;
  especieNueva: string;
  fechaHora: string;
  autorizadoPor: string;
}

// Lo mismo que valida el backend (RegistroCambio)
export const JUSTIFICACION_MINIMA = 10;
export const JUSTIFICACION_MAXIMA = 2000;

export function listarHallazgosAuditoria() {
  return pedir<ResumenHallazgo[]>('/interno/historial/hallazgos');
}

export function consultarHistorial(hallazgoId: string) {
  return pedir<HistorialHallazgo>(`/interno/historial/hallazgos/${hallazgoId}`);
}

export function reclasificarHallazgo(hallazgoId: string, datos: DatosReclasificacion) {
  return pedir<ReclasificacionRegistrada>(`/interno/hallazgos/${hallazgoId}/reclasificaciones`, {
    method: 'POST',
    body: JSON.stringify(datos),
  });
}
