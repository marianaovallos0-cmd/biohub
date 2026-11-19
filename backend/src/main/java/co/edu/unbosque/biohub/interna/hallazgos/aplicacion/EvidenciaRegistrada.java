package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import co.edu.unbosque.biohub.interna.hallazgos.dominio.Evidencia;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.TipoEvidencia;

import java.util.UUID;

// Con el id se consulta después la evidencia por GET /interno/evidencias/{id} (HU-01, criterio 2)
public record EvidenciaRegistrada(UUID id, TipoEvidencia tipo, String descripcion, String nombreOriginal) {

    public static EvidenciaRegistrada de(Evidencia evidencia, String nombreOriginal) {
        return new EvidenciaRegistrada(evidencia.getId(), evidencia.getTipo(), evidencia.getDescripcion(), nombreOriginal);
    }
}
