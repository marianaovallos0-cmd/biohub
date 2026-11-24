package co.edu.unbosque.biohub.publica.consultapublica.dominio;

/** El texto que llegó no sirve para buscar (vacío o muy corto). */
public class BusquedaInvalidaException extends RuntimeException {

    public BusquedaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
