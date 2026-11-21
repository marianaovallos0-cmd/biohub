package co.edu.unbosque.biohub.interna.usuarios.web;

import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import co.edu.unbosque.biohub.interna.usuarios.aplicacion.GestionarRoles;
import co.edu.unbosque.biohub.interna.usuarios.aplicacion.GestionarUsuarios;
import co.edu.unbosque.biohub.interna.usuarios.aplicacion.NuevoUsuario;
import co.edu.unbosque.biohub.interna.usuarios.aplicacion.PermisoVista;
import co.edu.unbosque.biohub.interna.usuarios.aplicacion.RolVista;
import co.edu.unbosque.biohub.interna.usuarios.aplicacion.UsuarioVista;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * HU-15. Solo entra quien tenga el permiso GESTIONAR_USUARIOS (el administrador).
 * Si otro rol lo intenta, responde 403 y el intento queda registrado (HU-14).
 */
@RestController
@RequestMapping("/interno")
@PreAuthorize("@autorizacion.validarAcceso(principal, 'GESTIONAR_USUARIOS')")
public class UsuariosController {

    private final GestionarUsuarios usuarios;
    private final GestionarRoles roles;

    public UsuariosController(GestionarUsuarios usuarios, GestionarRoles roles) {
        this.usuarios = usuarios;
        this.roles = roles;
    }

    // ---------- Usuarios ----------

    @GetMapping("/usuarios")
    public List<UsuarioVista> listarUsuarios() {
        return usuarios.listar();
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioVista crear(@RequestBody SolicitudUsuario solicitud) {
        return usuarios.crear(new NuevoUsuario(
                solicitud.nombre(), solicitud.correo(), solicitud.contrasena(), solicitud.rolId()));
    }

    @PutMapping("/usuarios/{id}/rol")
    public UsuarioVista cambiarRol(@PathVariable UUID id, @RequestBody SolicitudCambioRol solicitud,
                                   @AuthenticationPrincipal UsuarioAutenticado yo) {
        return usuarios.cambiarRol(id, solicitud.rolId(), yo.id());
    }

    @PostMapping("/usuarios/{id}/activar")
    public UsuarioVista activar(@PathVariable UUID id) {
        return usuarios.activar(id);
    }

    @PostMapping("/usuarios/{id}/desactivar")
    public UsuarioVista desactivar(@PathVariable UUID id, @AuthenticationPrincipal UsuarioAutenticado yo) {
        return usuarios.desactivar(id, yo.id());
    }

    // ---------- Roles y permisos ----------

    @GetMapping("/roles")
    public List<RolVista> listarRoles() {
        return roles.listar();
    }

    @GetMapping("/permisos")
    public List<PermisoVista> listarPermisos() {
        return roles.listarPermisos();
    }

    @PutMapping("/roles/{id}/permisos/{codigo}")
    public RolVista agregarPermiso(@PathVariable UUID id, @PathVariable String codigo) {
        return roles.agregarPermiso(id, codigo);
    }

    @DeleteMapping("/roles/{id}/permisos/{codigo}")
    public RolVista quitarPermiso(@PathVariable UUID id, @PathVariable String codigo,
                                  @AuthenticationPrincipal UsuarioAutenticado yo) {
        return roles.quitarPermiso(id, codigo, yo.id());
    }
}
