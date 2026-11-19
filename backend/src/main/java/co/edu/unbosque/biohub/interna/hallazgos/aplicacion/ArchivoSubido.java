package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import java.io.IOException;
import java.io.InputStream;

// Un archivo que llegó en la petición, sin depender de Spring (MultipartFile se adapta en la capa web).
// El contenido solo se abre cuando todo está validado.
public record ArchivoSubido(String nombreOriginal, long tamanoBytes, Contenido contenido) {

    @FunctionalInterface
    public interface Contenido {
        InputStream abrir() throws IOException;
    }
}
