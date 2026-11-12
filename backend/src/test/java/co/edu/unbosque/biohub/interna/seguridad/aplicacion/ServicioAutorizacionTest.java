package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ServicioAutorizacionTest {

    private final ServicioAutorizacion autorizacion = new ServicioAutorizacion();
    private final UsuarioAutenticado camila = new UsuarioAutenticado(UUID.randomUUID(), "Camila",
            "auditor@biohub.test", NivelAutorizacion.AUDITOR, Set.of("VER_HISTORIAL", "VER_INTENTOS"));

    @Test
    void permiteLoQueSuRolTiene() {
        assertThat(autorizacion.validarAcceso(camila, "VER_INTENTOS")).isTrue();
    }

    @Test
    void rechazaLoQueSuRolNoTiene() {
        assertThat(autorizacion.validarAcceso(camila, "VER_COORDENADAS")).isFalse();
    }

    @Test
    void rechazaSiNoHayUsuario() {
        assertThat(autorizacion.validarAcceso(null, "VER_INTENTOS")).isFalse();
    }
}