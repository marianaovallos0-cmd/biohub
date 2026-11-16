package co.edu.unbosque.biohub.publica.consultapublica.dominio;

import java.util.UUID;

/** No hay ningún hallazgo con ese id en la vista pública. */
public class FichaNoEncontradaException extends RuntimeException {

    public FichaNoEncontradaException(UUID hallazgoId) {
        super("No existe una ficha pública para el hallazgo " + hallazgoId + ".");
    }
}
