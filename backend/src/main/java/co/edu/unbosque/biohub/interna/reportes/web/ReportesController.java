package co.edu.unbosque.biohub.interna.reportes.web;

import co.edu.unbosque.biohub.interna.reportes.aplicacion.ConsultarReportes;
import co.edu.unbosque.biohub.interna.reportes.aplicacion.DetalleReporte;
import co.edu.unbosque.biohub.interna.reportes.aplicacion.PedirReporte;
import co.edu.unbosque.biohub.interna.reportes.aplicacion.ResumenReporte;
import co.edu.unbosque.biohub.interna.reportes.dominio.MesInvalidoException;
import co.edu.unbosque.biohub.interna.reportes.dominio.Reporte;
import co.edu.unbosque.biohub.interna.reportes.dominio.ReporteNoEncontradoException;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// HU-18. Solo el director (permiso VER_REPORTES) pide y consulta los reportes de presión ecológica
@RestController
@RequestMapping("/interno/reportes")
public class ReportesController {

    private final PedirReporte pedirReporte;
    private final ConsultarReportes consultarReportes;

    public ReportesController(PedirReporte pedirReporte, ConsultarReportes consultarReportes) {
        this.pedirReporte = pedirReporte;
        this.consultarReportes = consultarReportes;
    }

    // Si el mes ya se había pedido devuelve el mismo reporte (criterio 3)
    @PostMapping
    @PreAuthorize("@autorizacion.validarAcceso(principal, 'VER_REPORTES')")
    public DetalleReporte pedir(@RequestBody SolicitudReporte solicitud,
                                @AuthenticationPrincipal UsuarioAutenticado director) {
        Reporte reporte = pedirReporte.ejecutar(solicitud.mes(), director);
        return consultarReportes.detalle(reporte.id());
    }

    @GetMapping
    @PreAuthorize("@autorizacion.validarAcceso(principal, 'VER_REPORTES')")
    public List<ResumenReporte> listar() {
        return consultarReportes.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@autorizacion.validarAcceso(principal, 'VER_REPORTES')")
    public DetalleReporte detalle(@PathVariable UUID id) {
        return consultarReportes.detalle(id);
    }

    @ExceptionHandler(MesInvalidoException.class)
    ProblemDetail mesInvalido(MesInvalidoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ReporteNoEncontradoException.class)
    ProblemDetail noEncontrado(ReporteNoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
