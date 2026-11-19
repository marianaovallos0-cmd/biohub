package co.edu.unbosque.biohub.interna.taxonomia.dominio;

// Solo se insertan fichas: nunca se editan ni se borran (ASR-5)
public interface RepositorioFichas {

    void guardar(FichaTaxonomica ficha);
}
