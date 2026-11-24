package co.edu.unbosque.biohub.publica.consultapublica.dominio;

import java.util.List;

/**
 * Lo que la consulta pública necesita leer. La implementación solo puede
 * usar la vista pública, que no tiene coordenadas.
 */
public interface RepositorioConsultaPublica {

    /**
     * Busca hallazgos cuyo nombre de especie (género, especie o nombre científico
     * completo) contenga el texto, sin importar mayúsculas.
     */
    List<ResultadoBusqueda> buscarPorNombreEspecie(String texto, int limite);
}
