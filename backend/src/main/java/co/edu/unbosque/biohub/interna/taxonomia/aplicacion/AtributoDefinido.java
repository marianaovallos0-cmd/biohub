package co.edu.unbosque.biohub.interna.taxonomia.aplicacion;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.DefinicionAtributo;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoDato;

import java.util.UUID;

public record AtributoDefinido(UUID id, String nombre, TipoDato tipoDato, boolean obligatorio) {

    public static AtributoDefinido de(DefinicionAtributo definicion) {
        return new AtributoDefinido(definicion.getId(), definicion.getNombre(),
                definicion.getTipoDato(), definicion.isObligatorio());
    }
}
