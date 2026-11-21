package co.edu.unbosque.biohub.interna.historial.dominio;

import java.time.OffsetDateTime;
import java.util.UUID;

// Un hallazgo en la lista del auditor: su clasificación vigente y cuántas versiones tiene (HU-06, criterio 3)
public record ResumenHallazgo(UUID id, OffsetDateTime fechaRegistro, String expedicion, String investigador,
                              String tipoEspecie, String especie, String familia, String departamento,
                              int versionVigente) {

    public boolean tieneVersionesAnteriores() {
        return versionVigente > 1;
    }
}
