package co.edu.unbosque.biohub.interna.seguridad.web;

import co.edu.unbosque.biohub.interna.seguridad.aplicacion.ConsultarIntentos;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.IntentoRegistrado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RangoFechasInvalidoException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/interno/intentos")
public class IntentosController {

    private final ConsultarIntentos consultarIntentos;

    public IntentosController(ConsultarIntentos consultarIntentos) {
        this.consultarIntentos = consultarIntentos;
    }

    @GetMapping
    @PreAuthorize("@autorizacion.validarAcceso(principal, 'VER_INTENTOS')")
    public List<IntentoRegistrado> consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return consultarIntentos.ejecutar(desde, hasta);
    }

    @ExceptionHandler(RangoFechasInvalidoException.class)
    ProblemDetail rangoInvalido(RangoFechasInvalidoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}