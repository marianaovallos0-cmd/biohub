package co.edu.unbosque.biohub.publica.integracion.dominio;

/** La petición no trae X-API-Key o la clave no es de ningún consumidor (401). */
public class ClaveInvalidaException extends RuntimeException {

    public ClaveInvalidaException(String mensaje) {
        super(mensaje);
    }
}
