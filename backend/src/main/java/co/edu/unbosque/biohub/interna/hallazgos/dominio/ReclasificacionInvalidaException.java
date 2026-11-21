package co.edu.unbosque.biohub.interna.hallazgos.dominio;

import java.util.List;

public class ReclasificacionInvalidaException extends RuntimeException {

    private final List<String> errores;

    public ReclasificacionInvalidaException(List<String> errores) {
        super("La reclasificación no se guardó porque hay datos por corregir.");
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
