package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import co.edu.unbosque.biohub.interna.hallazgos.dominio.TipoEvidencia;

public record EvidenciaSubida(TipoEvidencia tipo, String descripcion, ArchivoSubido archivo) {
}
