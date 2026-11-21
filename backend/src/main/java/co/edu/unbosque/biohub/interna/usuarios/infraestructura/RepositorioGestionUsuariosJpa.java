package co.edu.unbosque.biohub.interna.usuarios.infraestructura;

import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import co.edu.unbosque.biohub.interna.usuarios.dominio.RepositorioGestionUsuarios;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioGestionUsuariosJpa implements RepositorioGestionUsuarios {

    private final UsuarioGestionSpringData jpa;

    public RepositorioGestionUsuariosJpa(UsuarioGestionSpringData jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Usuario> listar() {
        return jpa.findAll();
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existeCorreo(String correo) {
        return jpa.existsByCorreoIgnoreCase(correo);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return jpa.save(usuario);
    }
}
