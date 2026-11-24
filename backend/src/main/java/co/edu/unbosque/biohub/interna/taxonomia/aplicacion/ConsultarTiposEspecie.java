package co.edu.unbosque.biohub.interna.taxonomia.aplicacion;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.RepositorioTaxonomia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConsultarTiposEspecie {

    private final RepositorioTaxonomia taxonomia;

    public ConsultarTiposEspecie(RepositorioTaxonomia taxonomia) {
        this.taxonomia = taxonomia;
    }

    @Transactional(readOnly = true)
    public List<TipoEspecieConAtributos> ejecutar() {
        return taxonomia.listarTipos().stream().map(TipoEspecieConAtributos::de).toList();
    }
}
