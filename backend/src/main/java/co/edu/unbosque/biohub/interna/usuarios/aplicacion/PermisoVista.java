package co.edu.unbosque.biohub.interna.usuarios.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.dominio.Permiso;

public record PermisoVista(String codigo, String descripcion) {

    public static PermisoVista de(Permiso permiso) {
        return new PermisoVista(permiso.getCodigo(), permiso.getDescripcion());
    }
}
