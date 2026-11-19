package co.edu.unbosque.biohub.interna.taxonomia.dominio;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.validacion.ValidadorArchivo;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.validacion.ValidadorFecha;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.validacion.ValidadorNumero;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.validacion.ValidadorTexto;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.validacion.ValidadorTipoDato;

// Cada tipo de dato trae su estrategia de validación (Strategy)
public enum TipoDato {

    NUMERO(new ValidadorNumero()),
    TEXTO(new ValidadorTexto()),
    FECHA(new ValidadorFecha()),
    ARCHIVO(new ValidadorArchivo());

    private final ValidadorTipoDato validador;

    TipoDato(ValidadorTipoDato validador) {
        this.validador = validador;
    }

    public ValidadorTipoDato validador() {
        return validador;
    }
}
