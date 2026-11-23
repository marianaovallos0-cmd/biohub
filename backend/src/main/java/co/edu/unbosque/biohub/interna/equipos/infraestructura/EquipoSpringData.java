package co.edu.unbosque.biohub.interna.equipos.infraestructura;

import co.edu.unbosque.biohub.interna.equipos.dominio.EquipoCampo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface EquipoSpringData extends JpaRepository<EquipoCampo, UUID> {

    boolean existsByCodigoIdentificacion(String codigoIdentificacion);

    List<EquipoCampo> findAllByOrderByCodigoIdentificacionAsc();
}
