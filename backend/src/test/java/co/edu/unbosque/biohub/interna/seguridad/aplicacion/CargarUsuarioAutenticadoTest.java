package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.RepositorioUsuariosFalso;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.rol;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.usuario;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.AUDITOR;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.INVESTIGADOR;
import static org.assertj.core.api.Assertions.assertThat;

class CargarUsuarioAutenticadoTest {

    private final RepositorioUsuariosFalso repositorio = new RepositorioUsuariosFalso();
    private final CargarUsuarioAutenticado cargar = new CargarUsuarioAutenticado(repositorio);

    @Test
    void cargaElRolYLosPermisosDelUsuario() {
        Usuario camila = usuario("Camila", "auditor@biohub.test", rol(AUDITOR, "VER_HISTORIAL", "VER_INTENTOS"));
        repositorio.guardar(camila);

        UsuarioAutenticado autenticado = cargar.ejecutar(camila.getId()).orElseThrow();

        assertThat(autenticado.rol()).isEqualTo(AUDITOR);
        assertThat(autenticado.permisos()).containsExactlyInAnyOrder("VER_HISTORIAL", "VER_INTENTOS");
    }

    @Test
    void siLeCambianElRolLaSiguienteCargaTraeLosPermisosNuevos() {
        Usuario laura = usuario("Laura", "investigador1@biohub.test", rol(INVESTIGADOR, "REGISTRAR_HALLAZGO"));
        repositorio.guardar(laura);
        assertThat(cargar.ejecutar(laura.getId()).orElseThrow().tienePermiso("VER_INTENTOS")).isFalse();

        laura.cambiarRol(rol(AUDITOR, "VER_INTENTOS"));

        assertThat(cargar.ejecutar(laura.getId()).orElseThrow().tienePermiso("VER_INTENTOS")).isTrue();
    }

    @Test
    void unUsuarioDesactivadoNoSeCarga() {
        Usuario laura = usuario("Laura", "investigador1@biohub.test", rol(INVESTIGADOR, "REGISTRAR_HALLAZGO"));
        repositorio.guardar(laura);
        laura.desactivar();

        assertThat(cargar.ejecutar(laura.getId())).isEmpty();
    }

    @Test
    void unIdQueNoExisteNoSeCarga() {
        assertThat(cargar.ejecutar(UUID.randomUUID())).isEmpty();
    }
}