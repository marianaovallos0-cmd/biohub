package co.edu.unbosque.biohub.interna.ubicaciones.web;

import co.edu.unbosque.biohub.interna.ubicaciones.dominio.UbicacionConsultada;

import java.util.UUID;

/**
 * Dos formas de JSON. La pública no tiene campos de latitud ni longitud, así que
 * un usuario sin permiso no recibe ni siquiera la llave "latitud" (HU-13, ASR-1).
 */
public sealed interface RespuestaUbicacion {

    static RespuestaUbicacion de(UbicacionConsultada u) {
        if (u.exacta()) {
            return new Exacta(u.hallazgoId(), u.nombreCientifico(), u.region(), u.departamento(),
                    u.especieProtegida(), true, u.latitud(), u.longitud());
        }
        return new Publica(u.hallazgoId(), u.nombreCientifico(), u.region(), u.departamento(),
                u.especieProtegida(), false);
    }

    record Exacta(UUID hallazgoId, String nombreCientifico, String region, String departamento,
                  boolean especieProtegida, boolean exacta, double latitud, double longitud)
            implements RespuestaUbicacion {
    }

    record Publica(UUID hallazgoId, String nombreCientifico, String region, String departamento,
                   boolean especieProtegida, boolean exacta) implements RespuestaUbicacion {
    }
}
