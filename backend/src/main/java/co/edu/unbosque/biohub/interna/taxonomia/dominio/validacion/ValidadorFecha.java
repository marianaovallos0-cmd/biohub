package co.edu.unbosque.biohub.interna.taxonomia.dominio.validacion;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// FECHA: formato AAAA-MM-DD, que es lo que envía un <input type="date">
public class ValidadorFecha implements ValidadorTipoDato {

    @Override
    public boolean esValido(String valor) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        try {
            LocalDate.parse(valor.trim());
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    @Override
    public String mensajeError() {
        return "debe ser una fecha con formato AAAA-MM-DD";
    }
}
