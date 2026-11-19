package co.edu.unbosque.biohub.interna.taxonomia.dominio.validacion;

// TEXTO: cualquier texto que no esté vacío y no sea exageradamente largo
public class ValidadorTexto implements ValidadorTipoDato {

    static final int LARGO_MAXIMO = 2000;

    @Override
    public boolean esValido(String valor) {
        return valor != null && !valor.isBlank() && valor.length() <= LARGO_MAXIMO;
    }

    @Override
    public String mensajeError() {
        return "debe ser un texto de máximo " + LARGO_MAXIMO + " caracteres";
    }
}
