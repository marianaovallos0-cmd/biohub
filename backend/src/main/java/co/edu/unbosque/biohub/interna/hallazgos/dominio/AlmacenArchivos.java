package co.edu.unbosque.biohub.interna.hallazgos.dominio;

import java.io.InputStream;
import java.util.Optional;

public interface AlmacenArchivos {

    // Ruta nueva con nombre UUID, sin escribir nada todavía
    String nuevaRuta(String extension);

    // Si la transacción se deshace, el archivo se borra para no dejar basura.
    void guardar(String ruta, InputStream contenido);

    Optional<InputStream> leer(String ruta);
}
