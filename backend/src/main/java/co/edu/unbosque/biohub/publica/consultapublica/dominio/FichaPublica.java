package co.edu.unbosque.biohub.publica.consultapublica.dominio;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * HU-10: la ficha de una especie tal como la ve un visitante sin cuenta.
 * Trae la clasificación vigente y, si existe, la ubicación pública.
 * Es Serializable porque la ficha se guarda en Redis.
 */
public record FichaPublica(
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
        OffsetDateTime fechaRegistro,
        UbicacionPublica ubicacion) implements Serializable {
}
