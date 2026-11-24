import { pedir } from '@/api/cliente';

// HU-04: equipos de campo y su investigador responsable (solo el administrador)
export type TipoEquipo = 'DRON' | 'CAMARA_TRAMPA' | 'HIDROFONO' | 'SENSOR';

export interface Equipo {
  id: string;
  codigoIdentificacion: string;
  tipo: TipoEquipo;
  estado: string;
  investigadorId: string;
  responsable: string;
}

export interface Responsable {
  id: string;
  nombre: string;
  correo: string;
}

export const NOMBRE_TIPO_EQUIPO: Record<TipoEquipo, string> = {
  DRON: 'Dron',
  CAMARA_TRAMPA: 'Cámara trampa',
  HIDROFONO: 'Hidrófono',
  SENSOR: 'Sensor',
};

export function listarEquipos() {
  return pedir<Equipo[]>('/interno/equipos');
}

export function listarResponsables() {
  return pedir<Responsable[]>('/interno/equipos/responsables');
}

export function registrarEquipo(equipo: { codigoIdentificacion: string; tipo: TipoEquipo; investigadorId: string | null }) {
  return pedir<Equipo>('/interno/equipos', { method: 'POST', body: JSON.stringify(equipo) });
}

// HU-05: los equipos de un investigador. Si la lista viene vacía, la pantalla dice que no tiene equipos
export interface EquiposDeInvestigador {
  investigador: Responsable;
  equipos: Equipo[];
}

export function consultarEquiposDe(investigadorId: string) {
  return pedir<EquiposDeInvestigador>(`/interno/equipos/responsables/${investigadorId}`);
}
