package co.edu.unbosque.biohub.publica.integracion.dominio;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Un hallazgo tal como lo recibe un consumidor externo: la misma información de la
 * vista pública, sin coordenadas (ASR-1). Es Serializable porque se guarda en Redis.
 */
public record HallazgoExterno(
        UUID hallazgoId,
        String nombreCientifico,
        String tipoEspecie,
        String reino,
        String filo,
        String clase,
        String orden,
        String familia,
        String genero,
        String especie,
        String region,
        String departamento,
        OffsetDateTime fechaRegistro) implements Serializable {
}
