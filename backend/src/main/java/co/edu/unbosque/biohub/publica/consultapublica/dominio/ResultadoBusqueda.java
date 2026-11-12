package co.edu.unbosque.biohub.publica.consultapublica.dominio;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Un hallazgo tal como lo puede ver el público: su clasificación vigente
 * y la región, pero nunca las coordenadas (ASR-1).
 * Es Serializable porque la búsqueda se guarda en Redis.
 */
public record ResultadoBusqueda(
        UUID hallazgoId,
        String nombreCientifico,
        String tipoEspecie,
        String clase,
        String orden,
        String familia,
        String region,
        String departamento,
        OffsetDateTime fechaRegistro) implements Serializable {
}
