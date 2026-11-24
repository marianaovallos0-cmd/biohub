package co.edu.unbosque.biohub.interna.taxonomia.aplicacion;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.DefinicionAtributo;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.RepositorioTaxonomia;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecieNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// HU-03. Agrega un atributo a un tipo de especie con el sistema corriendo.
// Es un INSERT en definicion_atributo: no se recompila, no se reinicia y no se cambia ninguna tabla (ASR-7).
// El formulario lo muestra en la siguiente carga porque se arma con GET /interno/tipos-especie (HU-02).
@Service
public class AgregarAtributo {

    private final RepositorioTaxonomia taxonomia;

    public AgregarAtributo(RepositorioTaxonomia taxonomia) {
        this.taxonomia = taxonomia;
    }

    @Transactional
    public AtributoDefinido ejecutar(UUID tipoEspecieId, NuevoAtributo nuevo) {
        TipoEspecie tipo = taxonomia.buscarTipo(tipoEspecieId).orElseThrow(TipoEspecieNoEncontradoException::new);
        DefinicionAtributo definicion = tipo.agregarAtributo(nuevo.nombre(), nuevo.tipoDato(), nuevo.obligatorio());
        taxonomia.guardarDefinicion(definicion);
        return AtributoDefinido.de(definicion);
    }
}
