package co.edu.unbosque.biohub.interna.usuarios.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;

import java.util.UUID;

/** Un usuario tal como lo ve el administrador. Nunca lleva la contraseña ni su hash. */
public record UsuarioVista(UUID id, String nombre, String correo, boolean activo,
                           UUID rolId, String rol, String nivel) {

    public static UsuarioVista de(Usuario usuario) {
        return new UsuarioVista(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.isActivo(),
                usuario.getRol().getId(), usuario.getRol().getNombre(),
                usuario.getRol().getNivelAutorizacion().name());
    }
}
