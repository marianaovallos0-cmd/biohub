package co.edu.unbosque.biohub.interna.usuarios.dominio;

import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que la gestión de usuarios necesita. Va aparte de RepositorioUsuarios (seguridad),
 * que solo lee para el login: así cada componente depende solo de lo que usa.
 * No hay método para borrar: un usuario se desactiva (HU-15).
 */
public interface RepositorioGestionUsuarios {

    List<Usuario> listar();

    Optional<Usuario> buscarPorId(UUID id);

    boolean existeCorreo(String correo);

    Usuario guardar(Usuario usuario);
}
