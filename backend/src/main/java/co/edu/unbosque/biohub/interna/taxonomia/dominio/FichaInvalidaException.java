package co.edu.unbosque.biohub.interna.taxonomia.dominio;

import java.util.List;

public class FichaInvalidaException extends RuntimeException {

    private final List<String> errores;

    public FichaInvalidaException(List<String> errores) {
        super("La ficha taxonómica tiene datos inválidos.");
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
