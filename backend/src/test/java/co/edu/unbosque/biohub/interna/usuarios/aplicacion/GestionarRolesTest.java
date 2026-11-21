package co.edu.unbosque.biohub.interna.usuarios.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import co.edu.unbosque.biohub.interna.usuarios.AlmacenEnMemoria;
import co.edu.unbosque.biohub.interna.usuarios.dominio.ConflictoException;
import co.edu.unbosque.biohub.interna.usuarios.dominio.NoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestionarRolesTest {

    private AlmacenEnMemoria almacen;
    private GestionarRoles gestionar;
    private Usuario admin;
    private Usuario laura;
    private Usuario andres;

    @BeforeEach
    void preparar() {
        almacen = new AlmacenEnMemoria();
        almacen.reiniciar();
        admin = almacen.agregarUsuario("Admin BioHub", "admin@biohub.test", almacen.administrador);
        laura = almacen.agregarUsuario("Laura Gómez", "investigador1@biohub.test", almacen.investigador);
        andres = almacen.agregarUsuario("Andrés Rojas", "investigador2@biohub.test", almacen.investigador);
        gestionar = new GestionarRoles(almacen, almacen);
    }

    // ---------- Criterio 2: el cambio aplica a todos los del rol ----------

    @Test
    void agregarUnPermisoAlRolLoDaATodosSusUsuarios() {
        RolVista rol = gestionar.agregarPermiso(almacen.investigador.getId(), "VER_HISTORIAL");

        assertThat(rol.permisos()).contains("VER_HISTORIAL");
        assertThat(laura.tienePermiso("VER_HISTORIAL")).isTrue();
        assertThat(andres.tienePermiso("VER_HISTORIAL")).isTrue();
        // Los de otros roles no cambian
        assertThat(admin.tienePermiso("VER_HISTORIAL")).isFalse();
    }

    @Test
    void quitarUnPermisoAlRolSeLoQuitaATodosSusUsuarios() {
        gestionar.quitarPermiso(almacen.investigador.getId(), "REGISTRAR_HALLAZGO", admin.getId());

        assertThat(laura.tienePermiso("REGISTRAR_HALLAZGO")).isFalse();
        assertThat(andres.tienePermiso("REGISTRAR_HALLAZGO")).isFalse();
    }

    @Test
    void agregarDosVecesElMismoPermisoNoLoDuplica() {
        gestionar.agregarPermiso(almacen.investigador.getId(), "VER_HISTORIAL");
        RolVista rol = gestionar.agregarPermiso(almacen.investigador.getId(), "VER_HISTORIAL");

        assertThat(rol.permisos()).containsOnlyOnce("VER_HISTORIAL");
    }

    @Test
    void unPermisoORolQueNoExistenLanzanNoEncontrado() {
        assertThatThrownBy(() -> gestionar.agregarPermiso(almacen.investigador.getId(), "VOLAR"))
                .isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> gestionar.agregarPermiso(UUID.randomUUID(), "VER_HISTORIAL"))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void elAdministradorNoPuedeQuitarleASuRolLaGestionDeUsuarios() {
        assertThatThrownBy(() -> gestionar.quitarPermiso(
                almacen.administrador.getId(), "GESTIONAR_USUARIOS", admin.getId()))
                .isInstanceOf(ConflictoException.class);

        assertThat(admin.tienePermiso("GESTIONAR_USUARIOS")).isTrue();
    }

    @Test
    void listaLosRolesConSusPermisosYElCatalogo() {
        assertThat(gestionar.listar()).extracting(RolVista::nombre)
                .containsExactly("Administrador del sistema", "Auditor científico", "Investigador de campo");
        assertThat(gestionar.listarPermisos()).hasSize(AlmacenEnMemoria.CATALOGO.length);
    }
}
