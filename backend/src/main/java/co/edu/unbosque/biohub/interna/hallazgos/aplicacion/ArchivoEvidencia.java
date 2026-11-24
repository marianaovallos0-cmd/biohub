package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import java.io.InputStream;

public record ArchivoEvidencia(String nombre, String tipoContenido, InputStream contenido) {
}
