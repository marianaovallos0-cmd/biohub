package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.DatosFicha;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SolicitudRegistro(String expedicion, DatosFicha ficha, DatosUbicacion ubicacion,
                                List<EvidenciaSubida> evidencias, Map<UUID, ArchivoSubido> archivosAtributos) {

    public SolicitudRegistro {
        evidencias = evidencias == null ? List.of() : List.copyOf(evidencias);
        archivosAtributos = archivosAtributos == null ? Map.of() : Map.copyOf(archivosAtributos);
    }
}
