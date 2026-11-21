package co.edu.unbosque.biohub.interna.hallazgos.web;

import co.edu.unbosque.biohub.interna.hallazgos.aplicacion.ReclasificacionRegistrada;
import co.edu.unbosque.biohub.interna.hallazgos.aplicacion.ReclasificarHallazgo;
import co.edu.unbosque.biohub.interna.hallazgos.aplicacion.SolicitudReclasificacion;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.HallazgoNoEncontradoException;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.ReclasificacionInvalidaException;
import co.edu.unbosque.biohub.interna.historial.dominio.VersionDuplicadaException;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

// HU-06. POST /interno/hallazgos/{id}/reclasificaciones. Solo el auditor (RECLASIFICAR_HALLAZGO).
@RestController
@RequestMapping("/interno/hallazgos/{hallazgoId}/reclasificaciones")
public class ReclasificacionController {

    private final ReclasificarHallazgo reclasificar;

    public ReclasificacionController(ReclasificarHallazgo reclasificar) {
        this.reclasificar = reclasificar;
    }

    @PostMapping
    @PreAuthorize("@autorizacion.validarAcceso(principal, 'RECLASIFICAR_HALLAZGO')")
    public ResponseEntity<ReclasificacionRegistrada> reclasificar(
            @PathVariable UUID hallazgoId,
            @RequestBody SolicitudReclasificacion solicitud,
            @AuthenticationPrincipal UsuarioAutenticado auditor) {
        ReclasificacionRegistrada registrada = reclasificar.ejecutar(hallazgoId, solicitud, auditor);
        return ResponseEntity.created(URI.create("/interno/historial/hallazgos/" + hallazgoId)).body(registrada);
    }

    @ExceptionHandler(ReclasificacionInvalidaException.class)
    ProblemDetail invalida(ReclasificacionInvalidaException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problema.setProperty("errores", e.getErrores());
        return problema;
    }

    @ExceptionHandler(HallazgoNoEncontradoException.class)
    ProblemDetail noEncontrado(HallazgoNoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(VersionDuplicadaException.class)
    ProblemDetail concurrente(VersionDuplicadaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
