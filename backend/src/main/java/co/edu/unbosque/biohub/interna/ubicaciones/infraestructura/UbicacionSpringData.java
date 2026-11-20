package co.edu.unbosque.biohub.interna.ubicaciones.infraestructura;

import co.edu.unbosque.biohub.interna.ubicaciones.dominio.Ubicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface UbicacionSpringData extends JpaRepository<Ubicacion, UUID> {
}
