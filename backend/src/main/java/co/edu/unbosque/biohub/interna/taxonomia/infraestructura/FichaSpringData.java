package co.edu.unbosque.biohub.interna.taxonomia.infraestructura;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaTaxonomica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FichaSpringData extends JpaRepository<FichaTaxonomica, UUID> {

    Optional<FichaTaxonomica> findFirstByHallazgoIdOrderByNumeroVersionDesc(UUID hallazgoId);

    List<FichaTaxonomica> findByHallazgoIdOrderByNumeroVersionAsc(UUID hallazgoId);
}
