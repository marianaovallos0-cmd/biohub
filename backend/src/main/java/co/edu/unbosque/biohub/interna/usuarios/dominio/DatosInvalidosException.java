package co.edu.unbosque.biohub.interna.usuarios.dominio;

/** Faltan datos o no son válidos, por ejemplo un usuario sin rol (400). */
public class DatosInvalidosException extends RuntimeException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
