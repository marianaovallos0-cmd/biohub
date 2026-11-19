package co.edu.unbosque.biohub.interna.taxonomia.aplicacion;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;

import java.util.List;
import java.util.UUID;

public record TipoEspecieConAtributos(UUID id, String nombre, List<AtributoDefinido> atributos) {

    public static TipoEspecieConAtributos de(TipoEspecie tipo) {
        return new TipoEspecieConAtributos(tipo.getId(), tipo.getNombre(),
                tipo.getDefiniciones().stream().map(AtributoDefinido::de).toList());
    }
}
