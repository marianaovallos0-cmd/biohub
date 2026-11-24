package co.edu.unbosque.biohub.publica.integracion.dominio;

import java.util.UUID;

public class HallazgoNoEncontradoException extends RuntimeException {

    public HallazgoNoEncontradoException(UUID hallazgoId) {
        super("No existe el hallazgo " + hallazgoId + ".");
    }
}
