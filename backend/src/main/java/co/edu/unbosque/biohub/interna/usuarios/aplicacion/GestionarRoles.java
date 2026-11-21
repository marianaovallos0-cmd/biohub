package co.edu.unbosque.biohub.interna.usuarios.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.dominio.Permiso;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Rol;
import co.edu.unbosque.biohub.interna.usuarios.dominio.ConflictoException;
import co.edu.unbosque.biohub.interna.usuarios.dominio.NoEncontradoException;
import co.edu.unbosque.biohub.interna.usuarios.dominio.RepositorioGestionUsuarios;
import co.edu.unbosque.biohub.interna.usuarios.dominio.RepositorioRoles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * HU-15: el administrador agrega o quita permisos de un rol.
 * Como el token solo lleva el id y los permisos se leen de la base en cada petición
 * (ConvertidorJwt), el cambio aplica de inmediato a todos los usuarios con ese rol.
 */
@Service
public class GestionarRoles {

    // Es el permiso que deja entrar a esta pantalla
    public static final String PERMISO_GESTION = "GESTIONAR_USUARIOS";

    private final RepositorioRoles roles;
    private final RepositorioGestionUsuarios usuarios;

    public GestionarRoles(RepositorioRoles roles, RepositorioGestionUsuarios usuarios) {
        this.roles = roles;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<RolVista> listar() {
        return roles.listarRoles().stream()
                .sorted(Comparator.comparing(Rol::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(RolVista::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermisoVista> listarPermisos() {
        return roles.listarPermisos().stream()
                .sorted(Comparator.comparing(Permiso::getCodigo))
                .map(PermisoVista::de)
                .toList();
    }

    @Transactional
    public RolVista agregarPermiso(UUID rolId, String codigo) {
        Rol rol = buscar(rolId);
        Permiso permiso = roles.buscarPermiso(codigo)
                .orElseThrow(() -> new NoEncontradoException("No existe el permiso " + codigo + "."));
        rol.agregarPermiso(permiso);
        return RolVista.de(roles.guardarRol(rol));
    }

    @Transactional
    public RolVista quitarPermiso(UUID rolId, String codigo, UUID quienLoHace) {
        Rol rol = buscar(rolId);
        // Si el administrador se quita este permiso a sí mismo, nadie podría volver a entrar aquí
        boolean esSuPropioRol = usuarios.buscarPorId(quienLoHace)
                .map(u -> u.getRol().getId().equals(rol.getId()))
                .orElse(false);
        if (PERMISO_GESTION.equals(codigo) && esSuPropioRol) {
            throw new ConflictoException("No puedes quitarle a tu propio rol el permiso de gestionar usuarios.");
        }
        rol.quitarPermiso(codigo);
        return RolVista.de(roles.guardarRol(rol));
    }

    private Rol buscar(UUID rolId) {
        return roles.buscarRol(rolId)
                .orElseThrow(() -> new NoEncontradoException("No existe el rol " + rolId + "."));
    }
}
