package co.edu.unbosque.biohub.interna.historial.dominio;

import java.util.List;

public class CambioInvalidoException extends RuntimeException {

    private final List<String> errores;

    public CambioInvalidoException(List<String> errores) {
        super("El registro del cambio tiene datos inválidos.");
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
