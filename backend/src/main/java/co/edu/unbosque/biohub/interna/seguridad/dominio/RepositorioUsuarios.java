package co.edu.unbosque.biohub.interna.seguridad.dominio;

import java.util.Optional;
import java.util.UUID;

// Lo que seguridad necesita leer de los usuarios
public interface RepositorioUsuarios {

    Optional<Usuario> buscarPorCorreo(String correo);

    Optional<Usuario> buscarPorId(UUID id);
}