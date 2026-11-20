package co.edu.unbosque.biohub.interna.usuarios.infraestructura;

import co.edu.unbosque.biohub.interna.seguridad.dominio.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface RolSpringData extends JpaRepository<Rol, UUID> {
}
