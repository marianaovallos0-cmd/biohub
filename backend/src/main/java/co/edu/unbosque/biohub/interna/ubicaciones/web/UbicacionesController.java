package co.edu.unbosque.biohub.interna.ubicaciones.web;

import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import co.edu.unbosque.biohub.interna.ubicaciones.aplicacion.ServicioConsultaUbicacion;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.UbicacionNoEncontradaException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

/**
 * HU-13. Cualquier usuario interno con sesión puede pedir ubicaciones, pero solo el
 * de alto rango recibe coordenadas: la decisión la toma ServicioConsultaUbicacion.
 *   GET /interno/ubicaciones
 *   GET /interno/hallazgos/{hallazgoId}/ubicacion
 */
@RestController
@RequestMapping("/interno")
public class UbicacionesController {

    private final ServicioConsultaUbicacion consulta;

    public UbicacionesController(ServicioConsultaUbicacion consulta) {
        this.consulta = consulta;
    }

    @GetMapping("/ubicaciones")
    public List<RespuestaUbicacion> listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                            HttpServletRequest peticion) {
        return consulta.listar(usuario, recurso(peticion), peticion.getRemoteAddr()).stream()
                .map(RespuestaUbicacion::de).toList();
    }

    @GetMapping("/hallazgos/{hallazgoId}/ubicacion")
    public RespuestaUbicacion obtener(@PathVariable UUID hallazgoId,
                                       @AuthenticationPrincipal UsuarioAutenticado usuario,
                                       HttpServletRequest peticion) {
        return RespuestaUbicacion.de(
                consulta.obtenerUbicacion(hallazgoId, usuario, recurso(peticion), peticion.getRemoteAddr()));
    }

    @ExceptionHandler(UbicacionNoEncontradaException.class)
    ProblemDetail noEncontrada(UbicacionNoEncontradaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail idInvalido(MethodArgumentTypeMismatchException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "El id del hallazgo no es válido.");
    }

    private static String recurso(HttpServletRequest peticion) {
        return peticion.getMethod() + " " + peticion.getRequestURI();
    }
}
