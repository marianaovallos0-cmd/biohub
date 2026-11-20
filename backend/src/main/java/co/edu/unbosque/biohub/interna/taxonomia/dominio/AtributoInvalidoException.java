package co.edu.unbosque.biohub.interna.taxonomia.dominio;

// HU-03: el atributo no se puede agregar (nombre vacío, repetido o muy largo)
public class AtributoInvalidoException extends RuntimeException {

    public AtributoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
