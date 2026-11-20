package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record HallazgoRegistrado(UUID id, OffsetDateTime fechaRegistro, String autor,
                                 String especie, List<EvidenciaRegistrada> evidencias) {
}
