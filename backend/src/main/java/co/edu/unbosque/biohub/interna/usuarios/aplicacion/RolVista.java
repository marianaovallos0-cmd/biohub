package co.edu.unbosque.biohub.interna.usuarios.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.dominio.Permiso;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Rol;

import java.util.List;
import java.util.UUID;

/** Un rol con los códigos de sus permisos, ordenados. */
public record RolVista(UUID id, String nombre, String nivel, List<String> permisos) {

    public static RolVista de(Rol rol) {
        return new RolVista(rol.getId(), rol.getNombre(), rol.getNivelAutorizacion().name(),
                rol.getPermisos().stream().map(Permiso::getCodigo).sorted().toList());
    }
}
