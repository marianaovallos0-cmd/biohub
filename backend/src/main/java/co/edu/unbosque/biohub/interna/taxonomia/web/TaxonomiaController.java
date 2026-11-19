package co.edu.unbosque.biohub.interna.taxonomia.web;

import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.ConsultarTiposEspecie;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.TipoEspecieConAtributos;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HU-02. GET /interno/tipos-especie: cada tipo con sus atributos.
 */
@RestController
@RequestMapping("/interno/tipos-especie")
public class TaxonomiaController {

    private final ConsultarTiposEspecie consultarTipos;

    public TaxonomiaController(ConsultarTiposEspecie consultarTipos) {
        this.consultarTipos = consultarTipos;
    }

    @GetMapping
    @PreAuthorize("@autorizacion.validarAcceso(principal, 'REGISTRAR_HALLAZGO')")
    public List<TipoEspecieConAtributos> listar() {
        return consultarTipos.ejecutar();
    }
}
