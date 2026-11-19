package co.edu.unbosque.biohub.interna.taxonomia.infraestructura;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TipoEspecieSpringData extends JpaRepository<TipoEspecie, UUID> {

    List<TipoEspecie> findAllByOrderByNombreAsc();
}
