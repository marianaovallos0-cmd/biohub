package co.edu.unbosque.biohub.interna.taxonomia.dominio.validacion;

public interface ValidadorTipoDato {

    boolean esValido(String valor);

    /** Lo que se le dice al investigador cuando el valor no sirve. Ej.: "debe ser un número". */
    String mensajeError();
}
