package co.edu.unbosque.biohub.interna.taxonomia.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioTaxonomia {

    List<TipoEspecie> listarTipos();

    Optional<TipoEspecie> buscarTipo(UUID id);
}
