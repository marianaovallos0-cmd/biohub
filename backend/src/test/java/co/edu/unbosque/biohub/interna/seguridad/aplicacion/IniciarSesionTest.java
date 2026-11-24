package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.RepositorioUsuariosFalso;
import co.edu.unbosque.biohub.interna.seguridad.dominio.CredencialesInvalidasException;
import co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IniciarSesionTest {

    /** Doble tipo spy: devuelve un token fijo y anota para quién lo generó. */
    static class GeneradorEspia implements GeneradorToken {
        final List<UUID> generadosPara = new ArrayList<>();

        @Override
        public TokenEmitido generar(UUID usuarioId) {
            generadosPara.add(usuarioId);
            return new TokenEmitido("token-de-prueba", Instant.parse("2026-01-01T01:00:00Z"));
        }
    }

    private RepositorioUsuariosFalso repositorio;
    private GeneradorEspia generador;
    private IniciarSesion iniciarSesion;
    private Usuario camila;

    @BeforeEach
    void preparar() {
        camila = usuario("Camila Díaz", "auditor@biohub.test", rol(NivelAutorizacion.AUDITOR, "VER_HISTORIAL"));
        repositorio = new RepositorioUsuariosFalso();
        repositorio.guardar(camila);
        generador = new GeneradorEspia();
        iniciarSesion = new IniciarSesion(repositorio, BCRYPT, generador);
    }

    @Test
    void conCorreoYClaveCorrectosEntraConSuRol() {
        SesionIniciada sesion = iniciarSesion.ejecutar("auditor@biohub.test", CLAVE);

        assertThat(sesion.token()).isEqualTo("token-de-prueba");
        assertThat(sesion.nombre()).isEqualTo("Camila Díaz");
        assertThat(sesion.rol()).isEqualTo(NivelAutorizacion.AUDITOR);
        assertThat(generador.generadosPara).containsExactly(camila.getId());
    }

    @Test
    void conClaveIncorrectaNoEntra() {
        assertThatThrownBy(() -> iniciarSesion.ejecutar("auditor@biohub.test", "otra-clave"))
                .isInstanceOf(CredencialesInvalidasException.class);
        assertThat(generador.generadosPara).isEmpty();
    }

    @Test
    void conCorreoQueNoExisteNoEntra() {
        assertThatThrownBy(() -> iniciarSesion.ejecutar("nadie@biohub.test", CLAVE))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void unUsuarioDesactivadoNoEntraAunqueLaClaveSeaCorrecta() {
        camila.desactivar();

        assertThatThrownBy(() -> iniciarSesion.ejecutar("auditor@biohub.test", CLAVE))
                .isInstanceOf(CredencialesInvalidasException.class);
        assertThat(generador.generadosPara).isEmpty();
    }

    @Test
    void sinCorreoOSinClaveNoEntra() {
        assertThatThrownBy(() -> iniciarSesion.ejecutar(null, CLAVE))
                .isInstanceOf(CredencialesInvalidasException.class);
        assertThatThrownBy(() -> iniciarSesion.ejecutar("auditor@biohub.test", null))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}