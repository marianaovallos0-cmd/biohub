package co.edu.unbosque.biohub.interna.seguridad.infraestructura;

import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UsuarioSpringData extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);
}