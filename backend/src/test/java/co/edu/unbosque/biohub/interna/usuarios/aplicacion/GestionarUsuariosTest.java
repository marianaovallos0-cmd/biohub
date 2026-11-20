package co.edu.unbosque.biohub.interna.usuarios.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import co.edu.unbosque.biohub.interna.usuarios.AlmacenEnMemoria;
import co.edu.unbosque.biohub.interna.usuarios.dominio.ConflictoException;
import co.edu.unbosque.biohub.interna.usuarios.dominio.DatosInvalidosException;
import co.edu.unbosque.biohub.interna.usuarios.dominio.NoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestionarUsuariosTest {

    private AlmacenEnMemoria almacen;
    private GestionarUsuarios gestionar;
    private Usuario admin;
    private Usuario laura;

    @BeforeEach
    void preparar() {
        almacen = new AlmacenEnMemoria();
        almacen.reiniciar();
        admin = almacen.agregarUsuario("Admin BioHub", "admin@biohub.test", almacen.administrador);
        laura = almacen.agregarUsuario("Laura Gómez", "investigador1@biohub.test", almacen.investigador);
        gestionar = new GestionarUsuarios(almacen, almacen, DatosDePrueba.BCRYPT);
    }

    private NuevoUsuario nuevo(UUID rolId) {
        return new NuevoUsuario("Pedro Ruiz", "Pedro.Ruiz@BioHub.test", "clave-segura", rolId);
    }

    // ---------- Criterio 1: crear con rol ----------

    @Test
    void creaUnUsuarioActivoConElRolElegido() {
        UsuarioVista creado = gestionar.crear(nuevo(almacen.auditor.getId()));

        assertThat(creado.activo()).isTrue();
        assertThat(creado.rolId()).isEqualTo(almacen.auditor.getId());
        assertThat(creado.nivel()).isEqualTo("AUDITOR");
        // El usuario guardado tiene los permisos del rol
        Usuario guardado = almacen.buscarPorId(creado.id()).orElseThrow();
        assertThat(guardado.tienePermiso("VER_INTENTOS")).isTrue();
        assertThat(guardado.tienePermiso("GESTIONAR_USUARIOS")).isFalse();
    }

    @Test
    void laContrasenaSeGuardaCifradaYElCorreoEnMinusculas() {
        UsuarioVista creado = gestionar.crear(nuevo(almacen.auditor.getId()));
        Usuario guardado = almacen.buscarPorId(creado.id()).orElseThrow();

        assertThat(guardado.getCorreo()).isEqualTo("pedro.ruiz@biohub.test");
        assertThat(guardado.getContrasenaHash()).isNotEqualTo("clave-segura").startsWith("$2a$");
        assertThat(DatosDePrueba.BCRYPT.matches("clave-segura", guardado.getContrasenaHash())).isTrue();
    }

    // ---------- Criterio 3: sin rol no se guarda ----------

    @Test
    void unUsuarioSinRolNoSeGuarda() {
        assertThatThrownBy(() -> gestionar.crear(nuevo(null)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El usuario debe tener un rol.");

        assertThat(almacen.guardados).isZero();
    }

    @Test
    void unRolQueNoExisteTampocoSeGuarda() {
        assertThatThrownBy(() -> gestionar.crear(nuevo(UUID.randomUUID())))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessage("El rol elegido no existe.");
        assertThat(almacen.guardados).isZero();
    }

    // ---------- Validaciones ----------

    @Test
    void rechazaDatosIncompletosOInvalidos() {
        UUID rol = almacen.auditor.getId();
        assertThatThrownBy(() -> gestionar.crear(new NuevoUsuario(" ", "a@b.co", "clave-segura", rol)))
                .hasMessage("El nombre es obligatorio.");
        assertThatThrownBy(() -> gestionar.crear(new NuevoUsuario("Pedro", "no-es-correo", "clave-segura", rol)))
                .hasMessage("El correo no es válido.");
        assertThatThrownBy(() -> gestionar.crear(new NuevoUsuario("Pedro", "a@b.co", "corta", rol)))
                .hasMessage("La contraseña debe tener al menos 8 caracteres.");
        assertThat(almacen.guardados).isZero();
    }

    @Test
    void noPermiteDosUsuariosConElMismoCorreo() {
        assertThatThrownBy(() -> gestionar.crear(
                new NuevoUsuario("Otra Laura", "INVESTIGADOR1@biohub.test", "clave-segura", almacen.auditor.getId())))
                .isInstanceOf(ConflictoException.class);
    }

    // ---------- Modificar ----------

    @Test
    void cambiaElRolDeUnUsuario() {
        UsuarioVista cambiado = gestionar.cambiarRol(laura.getId(), almacen.auditor.getId(), admin.getId());

        assertThat(cambiado.nivel()).isEqualTo("AUDITOR");
        assertThat(laura.tienePermiso("VER_INTENTOS")).isTrue();
        assertThat(laura.tienePermiso("REGISTRAR_HALLAZGO")).isFalse();
    }

    @Test
    void noSePuedeDejarAUnUsuarioSinRol() {
        assertThatThrownBy(() -> gestionar.cambiarRol(laura.getId(), null, admin.getId()))
                .isInstanceOf(DatosInvalidosException.class);
        assertThat(laura.getRol()).isSameAs(almacen.investigador);
    }

    @Test
    void desactivarNoBorraYQuitaTodosLosPermisos() {
        gestionar.desactivar(laura.getId(), admin.getId());

        assertThat(almacen.buscarPorId(laura.getId())).isPresent();
        assertThat(laura.isActivo()).isFalse();
        assertThat(laura.tienePermiso("REGISTRAR_HALLAZGO")).isFalse();

        gestionar.activar(laura.getId());
        assertThat(laura.tienePermiso("REGISTRAR_HALLAZGO")).isTrue();
    }

    @Test
    void elAdministradorNoPuedeDesactivarseNiCambiarseElRol() {
        assertThatThrownBy(() -> gestionar.desactivar(admin.getId(), admin.getId()))
                .isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> gestionar.cambiarRol(admin.getId(), almacen.auditor.getId(), admin.getId()))
                .isInstanceOf(ConflictoException.class);
        assertThat(admin.isActivo()).isTrue();
    }

    @Test
    void unUsuarioQueNoExisteLanzaNoEncontrado() {
        assertThatThrownBy(() -> gestionar.activar(UUID.randomUUID()))
                .isInstanceOf(NoEncontradoException.class);
    }
}
