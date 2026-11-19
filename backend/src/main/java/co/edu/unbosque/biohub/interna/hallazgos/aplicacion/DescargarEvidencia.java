package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import co.edu.unbosque.biohub.interna.hallazgos.dominio.AccesoEvidenciaDenegadoException;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.AlmacenArchivos;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Evidencia;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.EvidenciaNoEncontradaException;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.ReglasArchivos;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioEvidencias;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioHallazgos;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

// HU-01: "las evidencias quedan asociadas al registro y se pueden consultar después".
// Una foto puede traer coordenadas GPS en sus metadatos, así que solo la ven el autor del hallazgo
// y quien tiene VER_COORDENADAS (alto rango). Los demás reciben 403 y el intento queda guardado (HU-14).
@Service
public class DescargarEvidencia {

    static final String PERMISO_COORDENADAS = "VER_COORDENADAS";

    private static final Map<String, String> TIPOS_CONTENIDO = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png",
            "pdf", "application/pdf", "mp3", "audio/mpeg", "wav", "audio/wav");

    private final RepositorioEvidencias evidencias;
    private final RepositorioHallazgos hallazgos;
    private final AlmacenArchivos almacen;

    public DescargarEvidencia(RepositorioEvidencias evidencias, RepositorioHallazgos hallazgos, AlmacenArchivos almacen) {
        this.evidencias = evidencias;
        this.hallazgos = hallazgos;
        this.almacen = almacen;
    }

    @Transactional(readOnly = true)
    public ArchivoEvidencia ejecutar(UUID evidenciaId, UsuarioAutenticado usuario) {
        Evidencia evidencia = evidencias.buscarPorId(evidenciaId).orElseThrow(EvidenciaNoEncontradaException::new);

        boolean esAutor = hallazgos.buscarPorId(evidencia.getHallazgoId())
                .map(h -> h.esAutor(usuario.id()))
                .orElse(false);
        if (!esAutor && !usuario.tienePermiso(PERMISO_COORDENADAS)) {
            throw new AccesoEvidenciaDenegadoException();
        }

        String ruta = evidencia.getUrlArchivo();
        InputStream contenido = almacen.leer(ruta).orElseThrow(EvidenciaNoEncontradaException::new);
        String nombre = ruta.substring(ruta.lastIndexOf('/') + 1);
        String tipo = TIPOS_CONTENIDO.getOrDefault(ReglasArchivos.extension(ruta), "application/octet-stream");
        return new ArchivoEvidencia(nombre, tipo, contenido);
    }
}
