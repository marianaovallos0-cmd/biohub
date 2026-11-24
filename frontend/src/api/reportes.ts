import { pedir } from '@/api/cliente';

// HU-18: lo que devuelven /interno/reportes y /interno/reportes/{id}
export interface ResumenReporte {
  id: string;
  mes: string;
  generado: boolean;
  fechaGeneracion: string | null;
}

export interface FilaReporte {
  departamento: string;
  totalHallazgos: number;
  hallazgosProtegidos: number;
  variacionPorcentual: number | null;
  enAlerta: boolean;
}

export interface DetalleReporte extends ResumenReporte {
  totalHallazgos: number;
  hallazgosProtegidos: number;
  departamentosEnAlerta: number;
  filas: FilaReporte[];
}

export function listarReportes() {
  return pedir<ResumenReporte[]>('/interno/reportes');
}

export function verReporte(id: string) {
  return pedir<DetalleReporte>(`/interno/reportes/${id}`);
}

// Si el mes ya se había pedido, el backend devuelve el mismo reporte (no lo recalcula)
export function pedirReporte(mes: string) {
  return pedir<DetalleReporte>('/interno/reportes', { method: 'POST', body: JSON.stringify({ mes }) });
}

// "2026-10" -> "octubre de 2026"
export function nombreMes(mes: string) {
  const [anio, numero] = mes.split('-').map(Number);
  return new Intl.DateTimeFormat('es-CO', { month: 'long', year: 'numeric' }).format(new Date(anio, numero - 1, 1));
}

// El último mes que ya terminó, en formato AAAA-MM (lo que pide el <input type="month">)
export function mesAnterior() {
  const hoy = new Date();
  const fecha = new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1);
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}`;
}
