package co.edu.unbosque.biohub.interna.ubicaciones.dominio;

import java.util.UUID;

public class UbicacionNoEncontradaException extends RuntimeException {

    public UbicacionNoEncontradaException(UUID hallazgoId) {
        super("El hallazgo " + hallazgoId + " no tiene ubicación registrada.");
    }
}
