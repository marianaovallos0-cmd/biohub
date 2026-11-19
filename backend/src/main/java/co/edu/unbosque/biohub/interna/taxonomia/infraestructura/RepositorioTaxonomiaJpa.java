package co.edu.unbosque.biohub.interna.taxonomia.infraestructura;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.RepositorioTaxonomia;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioTaxonomiaJpa implements RepositorioTaxonomia {

    private final TipoEspecieSpringData jpa;

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
}
