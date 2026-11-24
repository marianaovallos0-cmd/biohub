package co.edu.unbosque.biohub.interna.hallazgos.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Reglas de HU-01 para cualquier archivo que sube el investigador
public final class ReglasArchivos {

    public static final long MAXIMO_BYTES = 10L * 1024 * 1024;  // 10 MB

    private ReglasArchivos() { }

    public static String extension(String nombreArchivo) {
        if (nombreArchivo == null) {
            return "";
        }
        int punto = nombreArchivo.lastIndexOf('.');
        return punto < 0 ? "" : nombreArchivo.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    // Tamaño: ni vacío ni de más de 10 MB.
    public static List<String> erroresDeTamano(String etiqueta, long bytes) {
        List<String> errores = new ArrayList<>();
        if (bytes <= 0) {
            errores.add(etiqueta + ": el archivo está vacío.");
        } else if (bytes > MAXIMO_BYTES) {
            errores.add(etiqueta + ": pesa más de 10 MB.");
        }
        return errores;
    }

    // Tamaño y formato de una evidencia según su tipo.
    public static List<String> erroresDeEvidencia(String etiqueta, TipoEvidencia tipo, String nombreArchivo, long bytes) {
        List<String> errores = new ArrayList<>();
        if (tipo == null) {
            errores.add(etiqueta + ": falta el tipo de evidencia.");
        } else if (!tipo.aceptaExtension(extension(nombreArchivo))) {
            errores.add(etiqueta + ": para " + tipo.name() + " solo se acepta " + tipo.formatos() + ".");
        }
        errores.addAll(erroresDeTamano(etiqueta, bytes));
        return errores;
    }
}
