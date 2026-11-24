package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Permiso;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

//Quién hizo la petición, con su rol y permisos leídos de la base en ese momento
    public record UsuarioAutenticado(UUID id, String nombre, String correo, NivelAutorizacion rol, Set<String> permisos) {
    
        public static UsuarioAutenticado de(Usuario usuario) {
            Set<String> codigos = usuario.getRol().getPermisos().stream()
                    .map(Permiso::getCodigo)
                    .collect(Collectors.toUnmodifiableSet());
            return new UsuarioAutenticado(usuario.getId(), usuario.getNombre(), usuario.getCorreo(),
                    usuario.getRol().getNivelAutorizacion(), codigos);
        }

        public boolean tienePermiso(String codigo) {
            return permisos.contains(codigo);
        }

}
