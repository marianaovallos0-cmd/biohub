package co.edu.unbosque.biohub.interna.hallazgos.dominio;

import java.util.List;

public class HallazgoInvalidoException extends RuntimeException {

    private final List<String> errores;

    public HallazgoInvalidoException(List<String> errores) {
        super("El hallazgo no se guardó porque hay datos por corregir.");
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
