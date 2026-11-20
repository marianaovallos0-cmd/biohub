package co.edu.unbosque.biohub.interna.usuarios.infraestructura;

import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface UsuarioGestionSpringData extends JpaRepository<Usuario, UUID> {

    boolean existsByCorreoIgnoreCase(String correo);
}
