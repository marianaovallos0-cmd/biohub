package co.edu.unbosque.biohub.interna.seguridad.dominio;

public class RangoFechasInvalidoException extends RuntimeException {

    public RangoFechasInvalidoException() {
        super("La fecha inicial no puede ser posterior a la fecha final.");
    }
}