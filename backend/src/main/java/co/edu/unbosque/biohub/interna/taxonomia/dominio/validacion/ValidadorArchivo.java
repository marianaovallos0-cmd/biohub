package co.edu.unbosque.biohub.interna.taxonomia.dominio.validacion;

import java.util.Locale;
import java.util.Set;


public class ValidadorArchivo implements ValidadorTipoDato {

    public static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png", "pdf", "mp3", "wav");

    @Override
    public boolean esValido(String valor) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        String ruta = valor.trim();
        if (ruta.startsWith("/") || ruta.startsWith("\\") || ruta.contains("..") || ruta.contains(":")) {
            return false;
        }
        int punto = ruta.lastIndexOf('.');
        return punto > 0 && EXTENSIONES.contains(ruta.substring(punto + 1).toLowerCase(Locale.ROOT));
    }

    @Override
    public String mensajeError() {
        return "debe ser un archivo jpg, png, pdf, mp3 o wav";
    }
}
