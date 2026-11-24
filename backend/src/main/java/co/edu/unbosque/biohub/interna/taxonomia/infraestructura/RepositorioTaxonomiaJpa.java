package co.edu.unbosque.biohub.interna.taxonomia.infraestructura;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.DefinicionAtributo;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.RepositorioTaxonomia;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioTaxonomiaJpa implements RepositorioTaxonomia {

    private final TipoEspecieSpringData jpa;

    @PersistenceContext
    private EntityManager em;

    public RepositorioTaxonomiaJpa(TipoEspecieSpringData jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<TipoEspecie> listarTipos() {
        return jpa.findAllByOrderByNombreAsc();
    }

    @Override
    public Optional<TipoEspecie> buscarTipo(UUID id) {
        return jpa.findById(id);
    }

    // persist + flush: el id queda listo para la respuesta y un nombre repetido falla aquí (UNIQUE en la base)
    @Override
    public void guardarDefinicion(DefinicionAtributo definicion) {
        em.persist(definicion);
        em.flush();
    }
}
