package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.dominio.IntentoAccesoNoAutorizado;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IntentoRegistrado(UUID id, OffsetDateTime fechaHora, String usuario, String correo,
                                String rol, String recurso, String ip) {

    public static IntentoRegistrado de(IntentoAccesoNoAutorizado intento) {
        return new IntentoRegistrado(
                intento.getId(),
                intento.getFechaHora(),
                intento.getUsuario().getNombre(),
                intento.getUsuario().getCorreo(),
                intento.getUsuario().getRol().getNivelAutorizacion().name(),
                intento.getRecursoSolicitado(),
                intento.getIpOrigen());
    }
}