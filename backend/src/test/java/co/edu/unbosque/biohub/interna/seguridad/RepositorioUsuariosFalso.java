package co.edu.unbosque.biohub.interna.seguridad;

import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioUsuarios;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Doble tipo fake: guarda los usuarios en una lista en vez de PostgreSQL. */
public class RepositorioUsuariosFalso implements RepositorioUsuarios {

    private final List<Usuario> usuarios = new ArrayList<>();

    public void guardar(Usuario usuario) {
        usuarios.add(usuario);
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarios.stream().filter(u -> u.getCorreo().equalsIgnoreCase(correo)).findFirst();
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return usuarios.stream().filter(u -> u.getId().equals(id)).findFirst();
    }
}