package co.edu.unbosque.biohub.interna.taxonomia.infraestructura;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaTaxonomica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface FichaSpringData extends JpaRepository<FichaTaxonomica, UUID> {
}
