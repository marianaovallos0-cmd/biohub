package co.edu.unbosque.biohub.interna.taxonomia.infraestructura;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaTaxonomica;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.RepositorioFichas;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioFichasJpa implements RepositorioFichas {

    private final FichaSpringData jpa;

    public RepositorioFichasJpa(FichaSpringData jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(FichaTaxonomica ficha) {
        jpa.save(ficha);
    }
}
