package co.edu.unbosque.biohub.interna.taxonomia.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Solo se insertan fichas: nunca se editan ni se borran (ASR-5)
public interface RepositorioFichas {

    void guardar(FichaTaxonomica ficha);

    // La versión más reciente de la ficha de un hallazgo
    Optional<FichaTaxonomica> buscarVigente(UUID hallazgoId);

    // Todas las versiones de un hallazgo, de la 1 a la vigente (HU-08)
    List<FichaTaxonomica> listarVersiones(UUID hallazgoId);
}
