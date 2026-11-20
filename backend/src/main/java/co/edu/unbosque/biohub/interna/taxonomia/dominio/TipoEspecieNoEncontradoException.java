package co.edu.unbosque.biohub.interna.taxonomia.dominio;

public class TipoEspecieNoEncontradoException extends RuntimeException {

    public TipoEspecieNoEncontradoException() {
        super("El tipo de especie no existe.");
    }
}
