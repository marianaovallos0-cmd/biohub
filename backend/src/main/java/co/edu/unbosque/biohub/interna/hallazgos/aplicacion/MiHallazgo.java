package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MiHallazgo(UUID id, OffsetDateTime fechaRegistro, String expedicion, String especie,
                         List<EvidenciaRegistrada> evidencias) {
}
