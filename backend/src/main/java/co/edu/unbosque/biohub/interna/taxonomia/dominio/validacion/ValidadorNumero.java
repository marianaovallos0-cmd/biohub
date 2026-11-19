package co.edu.unbosque.biohub.interna.taxonomia.dominio.validacion;

import java.math.BigDecimal;

// NUMERO: acepta enteros y decimales con punto (24.5, -3, 0.75)
public class ValidadorNumero implements ValidadorTipoDato {

    @Override
    public boolean esValido(String valor) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        try {
            new BigDecimal(valor.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String mensajeError() {
        return "debe ser un número (use punto para los decimales)";
    }
}
