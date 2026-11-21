package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReclasificacionRegistrada(UUID hallazgoId, int versionAnterior, int versionNueva,
                                        String especieAnterior, String especieNueva,
                                        OffsetDateTime fechaHora, String autorizadoPor) {
}
