package co.edu.unbosque.biohub.interna.usuarios.dominio;

import co.edu.unbosque.biohub.interna.seguridad.dominio.Permiso;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Rol;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Roles y el catálogo de permisos que se les pueden dar. */
public interface RepositorioRoles {

    List<Rol> listarRoles();

    Optional<Rol> buscarRol(UUID id);

    Rol guardarRol(Rol rol);

    List<Permiso> listarPermisos();

    Optional<Permiso> buscarPermiso(String codigo);
}
