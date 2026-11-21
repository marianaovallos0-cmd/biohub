package co.edu.unbosque.biohub.interna.taxonomia.web;

import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.AgregarAtributo;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.AtributoDefinido;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.ConsultarTiposEspecie;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.NuevoAtributo;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.TipoEspecieConAtributos;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.AtributoInvalidoException;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecieNoEncontradoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

// HU-02. GET /interno/tipos-especie: cada tipo con sus atributos.
// HU-03. POST /interno/tipos-especie/{id}/atributos: agrega un atributo sin reiniciar nada.
@RestController
@RequestMapping("/interno/tipos-especie")
public class TaxonomiaController {

    private final ConsultarTiposEspecie consultarTipos;
    private final AgregarAtributo agregarAtributo;

    public TaxonomiaController(ConsultarTiposEspecie consultarTipos, AgregarAtributo agregarAtributo) {
        this.consultarTipos = consultarTipos;
        this.agregarAtributo = agregarAtributo;
    }

    // El auditor también los lee para reclasificar (HU-06)
    @GetMapping
    @PreAuthorize("@autorizacion.validarAcceso(principal, 'REGISTRAR_HALLAZGO')"
            + " or @autorizacion.validarAcceso(principal, 'RECLASIFICAR_HALLAZGO')")
    public List<TipoEspecieConAtributos> listar() {
        return consultarTipos.ejecutar();
    }

    @PostMapping("/{tipoId}/atributos")
    @PreAuthorize("@autorizacion.validarAcceso(principal, 'GESTIONAR_TAXONOMIA')")
    public ResponseEntity<AtributoDefinido> agregar(@PathVariable UUID tipoId, @RequestBody NuevoAtributo nuevo) {
        AtributoDefinido creado = agregarAtributo.ejecutar(tipoId, nuevo);
        return ResponseEntity.created(URI.create("/interno/tipos-especie/" + tipoId + "/atributos/" + creado.id()))
                .body(creado);
    }

    @ExceptionHandler(AtributoInvalidoException.class)
    ProblemDetail atributoInvalido(AtributoInvalidoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(TipoEspecieNoEncontradoException.class)
    ProblemDetail tipoNoEncontrado(TipoEspecieNoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // Dos personas agregan el mismo nombre al mismo tiempo: lo ataja el UNIQUE de la base
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail repetido(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Ese tipo de especie ya tiene un atributo con ese nombre.");
    }
}
