package co.edu.unbosque.biohub.interna.hallazgos.web;

import co.edu.unbosque.biohub.interna.hallazgos.aplicacion.ArchivoEvidencia;
import co.edu.unbosque.biohub.interna.hallazgos.aplicacion.ArchivoSubido;
import co.edu.unbosque.biohub.interna.hallazgos.aplicacion.DescargarEvidencia;
import co.edu.unbosque.biohub.interna.hallazgos.aplicacion.EvidenciaSubida;
import co.edu.unbosque.biohub.interna.hallazgos.aplicacion.HallazgoRegistrado;
import co.edu.unbosque.biohub.interna.hallazgos.aplicacion.RegistrarHallazgo;
import co.edu.unbosque.biohub.interna.hallazgos.aplicacion.SolicitudRegistro;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.AccesoEvidenciaDenegadoException;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.EvidenciaNoEncontradaException;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.HallazgoInvalidoException;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HU-01. Ruta interna, con login y permiso REGISTRAR_HALLAZGO.
 */
@RestController
@RequestMapping("/interno")
public class HallazgosController {

    private static final String PREFIJO_ATRIBUTO = "atributo-";

    private final RegistrarHallazgo registrarHallazgo;
    private final DescargarEvidencia descargarEvidencia;

    public HallazgosController(RegistrarHallazgo registrarHallazgo, DescargarEvidencia descargarEvidencia) {
        this.registrarHallazgo = registrarHallazgo;
        this.descargarEvidencia = descargarEvidencia;
    }

    @PostMapping(path = "/hallazgos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@autorizacion.validarAcceso(principal, 'REGISTRAR_HALLAZGO')")
    public ResponseEntity<HallazgoRegistrado> registrar(
            @RequestPart("datos") SolicitudHallazgo datos,
            @RequestPart(name = "evidencias", required = false) List<MultipartFile> archivosEvidencia,
            MultipartHttpServletRequest peticion,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        HallazgoRegistrado registrado = registrarHallazgo.ejecutar(
                new SolicitudRegistro(datos.expedicion(), datos.ficha(), datos.ubicacion(),
                        evidencias(datos, archivosEvidencia), archivosDeAtributos(peticion)),
                usuario);
        return ResponseEntity.created(URI.create("/interno/hallazgos/" + registrado.id())).body(registrado);
    }

    @GetMapping("/evidencias/{id}")
    public ResponseEntity<InputStreamResource> descargar(@PathVariable UUID id,
                                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        ArchivoEvidencia archivo;
        try {
            archivo = descargarEvidencia.ejecutar(id, usuario);
        } catch (AccesoEvidenciaDenegadoException e) {
            throw new AccessDeniedException(e.getMessage());
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(archivo.nombre()).build().toString())
                .body(new InputStreamResource(archivo.contenido()));
    }

    private static List<EvidenciaSubida> evidencias(SolicitudHallazgo datos, List<MultipartFile> archivos) {
        List<SolicitudHallazgo.DescripcionEvidencia> descripciones =
                datos.evidencias() == null ? List.of() : datos.evidencias();
        List<MultipartFile> partes = archivos == null ? List.of() : archivos;
        List<EvidenciaSubida> evidencias = new ArrayList<>();
        int total = Math.max(descripciones.size(), partes.size());
        for (int i = 0; i < total; i++) {
            SolicitudHallazgo.DescripcionEvidencia descripcion = i < descripciones.size() ? descripciones.get(i) : null;
            MultipartFile parte = i < partes.size() ? partes.get(i) : null;
            evidencias.add(new EvidenciaSubida(
                    descripcion == null ? null : descripcion.tipo(),
                    descripcion == null ? null : descripcion.descripcion(),
                    parte == null ? null : adaptar(parte)));
        }
        return evidencias;
    }

    private static Map<UUID, ArchivoSubido> archivosDeAtributos(MultipartHttpServletRequest peticion) {
        Map<UUID, ArchivoSubido> archivos = new HashMap<>();
        peticion.getFileMap().forEach((nombre, parte) -> {
            if (nombre.startsWith(PREFIJO_ATRIBUTO) && !parte.isEmpty()) {
                try {
                    archivos.put(UUID.fromString(nombre.substring(PREFIJO_ATRIBUTO.length())), adaptar(parte));
                } catch (IllegalArgumentException e) {
                    throw new HallazgoInvalidoException(List.of("El archivo «" + nombre + "» no corresponde a ningún atributo."));
                }
            }
        });
        return archivos;
    }

    private static ArchivoSubido adaptar(MultipartFile parte) {
        return new ArchivoSubido(parte.getOriginalFilename(), parte.getSize(), parte::getInputStream);
    }

    @ExceptionHandler(HallazgoInvalidoException.class)
    ProblemDetail hallazgoInvalido(HallazgoInvalidoException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problema.setProperty("errores", e.getErrores());
        return problema;
    }

    @ExceptionHandler(EvidenciaNoEncontradaException.class)
    ProblemDetail evidenciaNoEncontrada(EvidenciaNoEncontradaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
