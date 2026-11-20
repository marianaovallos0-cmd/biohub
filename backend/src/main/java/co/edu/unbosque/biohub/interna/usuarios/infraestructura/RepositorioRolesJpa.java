package co.edu.unbosque.biohub.interna.usuarios.infraestructura;

import co.edu.unbosque.biohub.interna.seguridad.dominio.Permiso;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Rol;
import co.edu.unbosque.biohub.interna.usuarios.dominio.RepositorioRoles;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Guardar un rol actualiza rol_permiso, que es donde viven sus permisos. */
@Repository
public class RepositorioRolesJpa implements RepositorioRoles {

    private final RolSpringData roles;
    private final PermisoSpringData permisos;

    public RepositorioRolesJpa(RolSpringData roles, PermisoSpringData permisos) {
        this.roles = roles;
        this.permisos = permisos;
    }

    @Override
    public List<Rol> listarRoles() {
        return roles.findAll();
    }

    @Override
    public Optional<Rol> buscarRol(UUID id) {
        return roles.findById(id);
    }

    @Override
    public Rol guardarRol(Rol rol) {
        return roles.save(rol);
    }

    @Override
    public List<Permiso> listarPermisos() {
        return permisos.findAll();
    }

    @Override
    public Optional<Permiso> buscarPermiso(String codigo) {
        return permisos.findByCodigo(codigo);
    }
}
