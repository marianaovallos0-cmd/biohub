import { pedir, pedirArchivo } from '@/api/cliente';

// HU-01: POST /interno/hallazgos (multipart)
export type TipoEvidencia = 'FOTOGRAFIA' | 'MUESTRA' | 'NOTA_CAMPO' | 'AUDIO';

export interface DatosHallazgo {
  expedicion: string;
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
  ubicacion: {
    latitud: number | null;
    longitud: number | null;
    region: string;
    departamento: string;
    especieProtegida: boolean;
  };
  evidencias: { tipo: TipoEvidencia; descripcion: string }[];
}

export interface EvidenciaRegistrada {
  id: string;
  tipo: TipoEvidencia;
  descripcion: string | null;
  nombreOriginal: string;
}

export interface HallazgoRegistrado {
  id: string;
  fechaRegistro: string;
  autor: string;
  especie: string;
  evidencias: EvidenciaRegistrada[];
}

// HU-01, criterio 2: consultar una evidencia después de guardarla.
// Pasa por GET /interno/evidencias/{id}, que valida permisos (solo el autor o alto rango).
// La pestaña se abre antes del fetch para que el navegador no la bloquee como ventana emergente.
export async function abrirEvidencia(id: string) {
  const pestana = window.open('', '_blank');
  try {
    const archivo = await pedirArchivo(`/interno/evidencias/${id}`);
    const url = URL.createObjectURL(archivo);
    if (pestana) pestana.location.href = url;
    else window.open(url, '_blank');
  } catch (error) {
    pestana?.close();
    throw error;
  }
}

// Mismas reglas que el backend (TipoEvidencia y ReglasArchivos)
export const FORMATOS_EVIDENCIA: Record<TipoEvidencia, string> = {
  FOTOGRAFIA: '.jpg,.jpeg,.png',
  MUESTRA: '.jpg,.jpeg,.png,.pdf',
  NOTA_CAMPO: '.jpg,.jpeg,.png,.pdf',
  AUDIO: '.mp3,.wav',
};

export const NOMBRE_EVIDENCIA: Record<TipoEvidencia, string> = {
  FOTOGRAFIA: 'Fotografía',
  MUESTRA: 'Muestra',
  NOTA_CAMPO: 'Nota de campo',
  AUDIO: 'Audio',
};

export const MAXIMO_BYTES = 10 * 1024 * 1024;
// Entre todos los archivos de un hallazgo: el mismo límite de Nginx y Spring
export const MAXIMO_TOTAL_BYTES = 60 * 1024 * 1024;
export const MAXIMO_EVIDENCIAS = 5;

// Lo que devuelve GET /interno/hallazgos/mios (HU-01, criterio 2)
export interface MiHallazgo {
  id: string;
  fechaRegistro: string;
  expedicion: string | null;
  especie: string;
  evidencias: EvidenciaRegistrada[];
}

export function listarMisHallazgos() {
  return pedir<MiHallazgo[]>('/interno/hallazgos/mios');
}

// Arma el multipart: "datos" (JSON), "evidencias" (en el mismo orden que datos.evidencias)
// y "atributo-{id}" para cada atributo de tipo ARCHIVO.
export function registrarHallazgo(datos: DatosHallazgo, archivosEvidencia: File[], archivosAtributos: Record<string, File>) {
  const formulario = new FormData();
  formulario.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
  archivosEvidencia.forEach((archivo) => formulario.append('evidencias', archivo));
  Object.entries(archivosAtributos).forEach(([id, archivo]) => formulario.append(`atributo-${id}`, archivo));
  return pedir<HallazgoRegistrado>('/interno/hallazgos', { method: 'POST', body: formulario });
}

// Departamentos de Colombia y su región natural, para no escribirlos a mano
export const DEPARTAMENTOS: Record<string, string> = {
  Amazonas: 'Amazonía', Antioquia: 'Andina', Arauca: 'Orinoquía', Atlántico: 'Caribe',
  'Bogotá D. C.': 'Andina', Bolívar: 'Caribe', Boyacá: 'Andina', Caldas: 'Andina',
  Caquetá: 'Amazonía', Casanare: 'Orinoquía', Cauca: 'Pacífica', Cesar: 'Caribe',
  Chocó: 'Pacífica', Córdoba: 'Caribe', Cundinamarca: 'Andina', Guainía: 'Amazonía',
  Guaviare: 'Amazonía', Huila: 'Andina', 'La Guajira': 'Caribe', Magdalena: 'Caribe',
  Meta: 'Orinoquía', Nariño: 'Pacífica', 'Norte de Santander': 'Andina', Putumayo: 'Amazonía',
  Quindío: 'Andina', Risaralda: 'Andina', 'San Andrés y Providencia': 'Insular', Santander: 'Andina',
  Sucre: 'Caribe', Tolima: 'Andina', 'Valle del Cauca': 'Pacífica', Vaupés: 'Amazonía', Vichada: 'Orinoquía',
};
