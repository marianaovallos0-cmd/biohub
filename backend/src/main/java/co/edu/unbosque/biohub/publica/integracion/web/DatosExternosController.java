package co.edu.unbosque.biohub.publica.integracion.web;

import co.edu.unbosque.biohub.publica.integracion.aplicacion.ConsultarDatosExternos;
import co.edu.unbosque.biohub.publica.integracion.dominio.HallazgoExterno;
import co.edu.unbosque.biohub.publica.integracion.dominio.HallazgoNoEncontradoException;
import co.edu.unbosque.biohub.publica.integracion.dominio.PaginaInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

/**
 * HU-16. Rutas para consumidores externos. Piden X-API-Key (FiltroApiKey).
 *   GET /externo/hallazgos?pagina=0&tamano=100
 *   GET /externo/hallazgos/{hallazgoId}
 * Cualquier otra ruta de /externo responde 403 (PerfilPublicaConfig).
 */
@RestController
@RequestMapping("/externo/hallazgos")
public class DatosExternosController {

    private final ConsultarDatosExternos consultar;

    public DatosExternosController(ConsultarDatosExternos consultar) {
        this.consultar = consultar;
    }

    @GetMapping
    public RespuestaPaginaExterna listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "" + ConsultarDatosExternos.TAMANO_POR_DEFECTO) int tamano) {
        return RespuestaPaginaExterna.de(pagina, tamano, consultar.listar(pagina, tamano));
    }

    @GetMapping("/{hallazgoId}")
    public HallazgoExterno buscar(@PathVariable UUID hallazgoId) {
        return consultar.buscar(hallazgoId);
    }

    @ExceptionHandler(HallazgoNoEncontradoException.class)
    ProblemDetail noEncontrado(HallazgoNoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(PaginaInvalidaException.class)
    ProblemDetail paginaInvalida(PaginaInvalidaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "El parámetro " + e.getName() + " no es válido.");
    }
}
