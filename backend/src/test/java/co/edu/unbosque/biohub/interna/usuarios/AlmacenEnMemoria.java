package co.edu.unbosque.biohub.interna.usuarios;

import co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba;
import co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Permiso;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioUsuarios;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Rol;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import co.edu.unbosque.biohub.interna.usuarios.dominio.RepositorioGestionUsuarios;
import co.edu.unbosque.biohub.interna.usuarios.dominio.RepositorioRoles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Doble tipo fake: usuarios, roles y permisos en listas en vez de PostgreSQL.
 * Implementa también RepositorioUsuarios (seguridad) para que el login y cada petición
 * vean los mismos datos que la gestión, como pasa con la base real.
 * Los usuarios guardan el mismo objeto Rol, así que un cambio en el rol lo ven todos.
 */
public class AlmacenEnMemoria implements RepositorioUsuarios, RepositorioGestionUsuarios, RepositorioRoles {

    public static final String[] CATALOGO = {
            "REGISTRAR_HALLAZGO", "GESTIONAR_TAXONOMIA", "RECLASIFICAR_HALLAZGO", "VER_HISTORIAL",
            "VER_INTENTOS", "VER_COORDENADAS", "VER_REPORTES", "GESTIONAR_EQUIPOS", "GESTIONAR_USUARIOS"};

    private final List<Usuario> usuarios = new ArrayList<>();
    private final List<Rol> roles = new ArrayList<>();
    private final List<Permiso> permisos = new ArrayList<>();
    public int guardados;

    public Rol investigador;
    public Rol auditor;
    public Rol administrador;

    /** Deja los datos como en R__datos_prueba.sql (solo lo que usan las pruebas). */
    public void reiniciar() {
        usuarios.clear();
        roles.clear();
        permisos.clear();
        guardados = 0;
        for (String codigo : CATALOGO) {
            Permiso permiso = new Permiso(codigo, "Permiso " + codigo);
            ReflectionTestUtils.setField(permiso, "id", UUID.randomUUID());
            permisos.add(permiso);
        }
        investigador = rol("Investigador de campo", NivelAutorizacion.INVESTIGADOR,
                "REGISTRAR_HALLAZGO", "GESTIONAR_TAXONOMIA");
        auditor = rol("Auditor científico", NivelAutorizacion.AUDITOR,
                "RECLASIFICAR_HALLAZGO", "VER_HISTORIAL", "VER_INTENTOS");
        administrador = rol("Administrador del sistema", NivelAutorizacion.ADMINISTRADOR,
                "GESTIONAR_EQUIPOS", "GESTIONAR_USUARIOS");
    }

    public Rol rol(String nombre, NivelAutorizacion nivel, String... codigos) {
        Rol rol = new Rol(nombre, nivel, new java.util.HashSet<>());
        ReflectionTestUtils.setField(rol, "id", UUID.randomUUID());
        for (String codigo : codigos) {
            rol.agregarPermiso(buscarPermiso(codigo).orElseThrow());
        }
        roles.add(rol);
        return rol;
    }

    public Usuario agregarUsuario(String nombre, String correo, Rol rol) {
        Usuario usuario = DatosDePrueba.usuario(nombre, correo, rol);
        usuarios.add(usuario);
        return usuario;
    }

    // ---------- RepositorioUsuarios y RepositorioGestionUsuarios ----------

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarios.stream().filter(u -> u.getCorreo().equalsIgnoreCase(correo.trim())).findFirst();
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return usuarios.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    @Override
    public List<Usuario> listar() {
        return List.copyOf(usuarios);
    }

    @Override
    public boolean existeCorreo(String correo) {
        return buscarPorCorreo(correo).isPresent();
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        guardados++;
        if (usuario.getId() == null) {
            ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        }
        if (!usuarios.contains(usuario)) {
            usuarios.add(usuario);
        }
        return usuario;
    }

    // ---------- RepositorioRoles ----------

    @Override
    public List<Rol> listarRoles() {
        return List.copyOf(roles);
    }

    @Override
    public Optional<Rol> buscarRol(UUID id) {
        return roles.stream().filter(r -> r.getId().equals(id)).findFirst();
    }

    @Override
    public Rol guardarRol(Rol rol) {
        return rol;
    }

    @Override
    public List<Permiso> listarPermisos() {
        return List.copyOf(permisos);
    }

    @Override
    public Optional<Permiso> buscarPermiso(String codigo) {
        return permisos.stream().filter(p -> p.getCodigo().equals(codigo)).findFirst();
    }
}
