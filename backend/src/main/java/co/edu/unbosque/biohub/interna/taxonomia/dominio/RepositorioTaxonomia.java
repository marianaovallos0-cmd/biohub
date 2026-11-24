package co.edu.unbosque.biohub.interna.taxonomia.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Lo que Taxonomía necesita leer y escribir de los tipos de especie
public interface RepositorioTaxonomia {

    List<TipoEspecie> listarTipos();

    Optional<TipoEspecie> buscarTipo(UUID id);

    // HU-03: un atributo nuevo es una fila nueva en definicion_atributo, no una columna nueva (ASR-7)
    void guardarDefinicion(DefinicionAtributo definicion);
}
