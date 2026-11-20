package co.edu.unbosque.biohub.interna.usuarios.infraestructura;

import co.edu.unbosque.biohub.interna.seguridad.dominio.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PermisoSpringData extends JpaRepository<Permiso, UUID> {

    Optional<Permiso> findByCodigo(String codigo);
}
