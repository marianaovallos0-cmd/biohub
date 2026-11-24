package co.edu.unbosque.biohub.interna.equipos.dominio;

import java.util.List;

public class EquipoInvalidoException extends RuntimeException {

    private final List<String> errores;

    public EquipoInvalidoException(List<String> errores) {
        super("El equipo no se guardó porque hay datos por corregir.");
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
