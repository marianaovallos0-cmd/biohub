package co.edu.unbosque.biohub.interna.hallazgos.infraestructura;

import co.edu.unbosque.biohub.interna.hallazgos.dominio.AlmacenArchivos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@Component
public class AlmacenArchivosDisco implements AlmacenArchivos {

    private static final Logger log = LoggerFactory.getLogger(AlmacenArchivosDisco.class);
    private static final String CARPETA = "evidencias";

    private final Path raiz;

    public AlmacenArchivosDisco(@Value("${biohub.archivos.raiz:/data}") String raiz) {
        this.raiz = Path.of(raiz).toAbsolutePath().normalize();
    }

    @Override
    public String nuevaRuta(String extension) {
        return CARPETA + "/" + UUID.randomUUID() + "." + extension;
    }

    @Override
    public void guardar(String ruta, InputStream contenido) {
        Path destino = resolver(ruta);
        try {
            Files.createDirectories(destino.getParent());
            Files.copy(contenido, destino);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo " + ruta, e);
        }
        borrarSiLaTransaccionSeDeshace(destino);
    }

    @Override
    public Optional<InputStream> leer(String ruta) {
        Path archivo = resolver(ruta);
        if (!Files.isRegularFile(archivo)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.newInputStream(archivo));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo " + ruta, e);
        }
    }

    // Ninguna ruta puede salirse de la raíz (ej. "../../etc/passwd")
    private Path resolver(String ruta) {
        Path archivo = raiz.resolve(ruta).normalize();
        if (!archivo.startsWith(raiz)) {
            throw new IllegalArgumentException("Ruta fuera de la carpeta de archivos: " + ruta);
        }
        return archivo;
    }

    private void borrarSiLaTransaccionSeDeshace(Path archivo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                if (estado != STATUS_COMMITTED) {
                    try {
                        Files.deleteIfExists(archivo);
                    } catch (IOException e) {
                        log.warn("No se pudo borrar el archivo {} tras deshacer la transacción", archivo, e);
                    }
                }
            }
        });
    }
}
