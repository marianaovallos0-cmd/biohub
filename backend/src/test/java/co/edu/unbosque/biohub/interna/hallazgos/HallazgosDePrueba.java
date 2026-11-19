package co.edu.unbosque.biohub.interna.hallazgos;

import co.edu.unbosque.biohub.interna.hallazgos.aplicacion.ArchivoSubido;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.AlmacenArchivos;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Evidencia;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Hallazgo;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioEvidencias;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioHallazgos;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.RepositorioUbicaciones;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.Ubicacion;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Dobles tipo fake de HU-01: guardan en listas y en un mapa en vez de PostgreSQL y del disco. */
public final class HallazgosDePrueba {

    private HallazgosDePrueba() { }

    public static ArchivoSubido archivo(String nombre, String contenido) {
        byte[] bytes = contenido.getBytes();
        return new ArchivoSubido(nombre, bytes.length, () -> new ByteArrayInputStream(bytes));
    }

    /** Un archivo que dice pesar "bytes" sin ocupar esa memoria; si alguien lo abre, la prueba falla. */
    public static ArchivoSubido archivoQueNoSeDebeAbrir(String nombre, long bytes) {
        return new ArchivoSubido(nombre, bytes, () -> {
            throw new AssertionError("No se debía leer " + nombre);
        });
    }

    public static Evidencia evidencia(UUID hallazgoId, String ruta) {
        Evidencia evidencia = new Evidencia(hallazgoId,
                co.edu.unbosque.biohub.interna.hallazgos.dominio.TipoEvidencia.FOTOGRAFIA, ruta, null, null);
        ReflectionTestUtils.setField(evidencia, "id", UUID.randomUUID());
        return evidencia;
    }

    public static class RepositorioHallazgosFalso implements RepositorioHallazgos {

        public final List<Hallazgo> guardados = new ArrayList<>();

        @Override
        public void guardar(Hallazgo hallazgo) {
            guardados.add(hallazgo);
        }

        @Override
        public Optional<Hallazgo> buscarPorId(UUID id) {
            return guardados.stream().filter(h -> h.getId().equals(id)).findFirst();
        }
    }

    public static class RepositorioEvidenciasFalso implements RepositorioEvidencias {

        public final List<Evidencia> guardadas = new ArrayList<>();

        @Override
        public void guardar(Evidencia evidencia) {
            if (evidencia.getId() == null) {
                ReflectionTestUtils.setField(evidencia, "id", UUID.randomUUID());
            }
            guardadas.add(evidencia);
        }

        @Override
        public Optional<Evidencia> buscarPorId(UUID id) {
            return guardadas.stream().filter(e -> e.getId().equals(id)).findFirst();
        }
    }

    public static class RepositorioUbicacionesFalso implements RepositorioUbicaciones {

        public final List<Ubicacion> guardadas = new ArrayList<>();

        @Override
        public void guardar(Ubicacion ubicacion) {
            guardadas.add(ubicacion);
        }
    }

    public static class AlmacenArchivosFalso implements AlmacenArchivos {

        public final Map<String, String> archivos = new LinkedHashMap<>();
        private int siguiente = 1;

        @Override
        public String nuevaRuta(String extension) {
            return "evidencias/archivo-" + (siguiente++) + "." + extension;
        }

        @Override
        public void guardar(String ruta, InputStream contenido) {
            try {
                archivos.put(ruta, new String(contenido.readAllBytes()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public Optional<InputStream> leer(String ruta) {
            return Optional.ofNullable(archivos.get(ruta)).map(c -> new ByteArrayInputStream(c.getBytes()));
        }
    }
}
