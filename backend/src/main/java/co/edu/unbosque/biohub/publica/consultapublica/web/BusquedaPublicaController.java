package co.edu.unbosque.biohub.publica.consultapublica.web;

import co.edu.unbosque.biohub.publica.consultapublica.aplicacion.BuscarEspeciesYHallazgos;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.BusquedaInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU-09. Ruta pública, no pide login: GET /publico/busqueda?q=ara
 */
@RestController
@RequestMapping("/publico/busqueda")
public class BusquedaPublicaController {

    private final BuscarEspeciesYHallazgos buscar;

    public BusquedaPublicaController(BuscarEspeciesYHallazgos buscar) {
        this.buscar = buscar;
    }

    @GetMapping
    public RespuestaBusqueda buscar(@RequestParam(name = "q", defaultValue = "") String q) {
        String consulta = q.trim();
        return RespuestaBusqueda.de(consulta, buscar.ejecutar(consulta));
    }

    @ExceptionHandler(BusquedaInvalidaException.class)
    ProblemDetail busquedaInvalida(BusquedaInvalidaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
