package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.RepositorioIntentosFalso;
import co.edu.unbosque.biohub.interna.seguridad.RepositorioUsuariosFalso;
import co.edu.unbosque.biohub.interna.seguridad.dominio.IntentoAccesoNoAutorizado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.rol;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.usuario;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.INVESTIGADOR;
import static org.assertj.core.api.Assertions.assertThat;

class ServicioAutorizacionTest {

    // 12 de noviembre, 3:00 p. m. en Bogotá
    private static final Clock RELOJ_FIJO =
            Clock.fixed(Instant.parse("2026-11-12T20:00:00Z"), ZoneId.of("America/Bogota"));

    private RepositorioUsuariosFalso usuarios;
    private RepositorioIntentosFalso intentos;
    private ServicioAutorizacion autorizacion;
    private Usuario laura;
    private UsuarioAutenticado lauraAutenticada;

    @BeforeEach
    void preparar() {
        laura = usuario("Laura Gómez", "investigador1@biohub.test", rol(INVESTIGADOR, "REGISTRAR_HALLAZGO"));
        usuarios = new RepositorioUsuariosFalso();
        usuarios.guardar(laura);
        intentos = new RepositorioIntentosFalso();
        lauraAutenticada = UsuarioAutenticado.de(laura);
        autorizacion = new ServicioAutorizacion(usuarios, intentos, RELOJ_FIJO);
    }

    @Test
    void permiteLoQueSuRolTiene() {
        assertThat(autorizacion.validarAcceso(lauraAutenticada, "REGISTRAR_HALLAZGO")).isTrue();
    }

    @Test
    void rechazaLoQueSuRolNoTiene() {
        assertThat(autorizacion.validarAcceso(lauraAutenticada, "VER_COORDENADAS")).isFalse();
    }

    @Test
    void rechazaSiNoHayUsuario() {
        assertThat(autorizacion.validarAcceso(null, "REGISTRAR_HALLAZGO")).isFalse();
    }

    @Test
    void guardaElIntentoConUsuarioRecursoIpYHora() {
        autorizacion.registrarIntento(lauraAutenticada, "GET /interno/hallazgos/1/coordenadas", "190.25.10.4");

        assertThat(intentos.guardados).hasSize(1);
        IntentoAccesoNoAutorizado intento = intentos.guardados.get(0);
        assertThat(intento.getUsuario()).isSameAs(laura);
        assertThat(intento.getRecursoSolicitado()).isEqualTo("GET /interno/hallazgos/1/coordenadas");
        assertThat(intento.getIpOrigen()).isEqualTo("190.25.10.4");
        assertThat(intento.getFechaHora()).isEqualTo(OffsetDateTime.parse("2026-11-12T15:00-05:00"));
    }

    @Test
    void sinUsuarioNoSeGuardaNada() {
        autorizacion.registrarIntento(null, "GET /interno/intentos", "190.25.10.4");

        assertThat(intentos.guardados).isEmpty();
    }

    @Test
    void unRecursoMuyLargoSeRecortaPorqueLaColumnaEsDe255() {
        autorizacion.registrarIntento(lauraAutenticada, "GET /" + "x".repeat(400), "190.25.10.4");

        assertThat(intentos.guardados.get(0).getRecursoSolicitado()).hasSize(255);
    }
}
