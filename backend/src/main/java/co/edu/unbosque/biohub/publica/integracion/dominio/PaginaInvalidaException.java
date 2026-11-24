package co.edu.unbosque.biohub.publica.integracion.dominio;

public class PaginaInvalidaException extends RuntimeException {

    public PaginaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
