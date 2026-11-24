package co.edu.unbosque.biohub.interna.seguridad.infraestructura;

import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioUsuarios;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository 
public class RepositorioUsuariosJpa implements RepositorioUsuarios {

    private final UsuarioSpringData jpa;

    public RepositorioUsuariosJpa(UsuarioSpringData jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
       return jpa.findByCorreoIgnoreCase(correo.trim());
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    
}
