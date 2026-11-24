package co.edu.unbosque.biohub.publica.consultapublica.web;

import co.edu.unbosque.biohub.publica.consultapublica.aplicacion.VerFichaPublica;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.FichaNoEncontradaException;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.FichaPublica;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

/**
 * HU-10. Ruta pública, no pide login: GET /publico/fichas/{hallazgoId}
 */
@RestController
@RequestMapping("/publico/fichas")
public class FichaPublicaController {

    private final VerFichaPublica verFicha;

    public FichaPublicaController(VerFichaPublica verFicha) {
        this.verFicha = verFicha;
    }

    @GetMapping("/{hallazgoId}")
    public FichaPublica ver(@PathVariable UUID hallazgoId) {
        return verFicha.ejecutar(hallazgoId);
    }

    @ExceptionHandler(FichaNoEncontradaException.class)
    ProblemDetail noEncontrada(FichaNoEncontradaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail idInvalido(MethodArgumentTypeMismatchException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "El id de la ficha no es válido.");
    }
}
