package co.edu.unbosque.biohub.interna.historial.web;

import co.edu.unbosque.biohub.interna.hallazgos.dominio.HallazgoNoEncontradoException;
import co.edu.unbosque.biohub.interna.historial.aplicacion.ConsultarHistorial;
import co.edu.unbosque.biohub.interna.historial.aplicacion.HistorialHallazgo;
import co.edu.unbosque.biohub.interna.historial.aplicacion.ListarHallazgosAuditoria;
import co.edu.unbosque.biohub.interna.historial.dominio.ResumenHallazgo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// HU-08. Solo el auditor científico (VER_HISTORIAL).
//   GET /interno/historial/hallazgos       -> lista con la versión vigente de cada uno
//   GET /interno/historial/hallazgos/{id}  -> todas las versiones del hallazgo
@RestController
@RequestMapping("/interno/historial/hallazgos")
public class HistorialController {

    private final ListarHallazgosAuditoria listar;
    private final ConsultarHistorial consultar;

    public HistorialController(ListarHallazgosAuditoria listar, ConsultarHistorial consultar) {
        this.listar = listar;
        this.consultar = consultar;
    }

    @GetMapping
    @PreAuthorize("@autorizacion.validarAcceso(principal, 'VER_HISTORIAL')")
    public List<ResumenHallazgo> listar() {
        return listar.ejecutar();
    }

    @GetMapping("/{hallazgoId}")
    @PreAuthorize("@autorizacion.validarAcceso(principal, 'VER_HISTORIAL')")
    public HistorialHallazgo historial(@PathVariable UUID hallazgoId) {
        return consultar.ejecutar(hallazgoId);
    }

    @ExceptionHandler(HallazgoNoEncontradoException.class)
    ProblemDetail noEncontrado(HallazgoNoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
