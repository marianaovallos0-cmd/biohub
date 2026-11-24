package co.edu.unbosque.biohub.interna.hallazgos.dominio;

public class EvidenciaNoEncontradaException extends RuntimeException {

    public EvidenciaNoEncontradaException() {
        super("La evidencia no existe o su archivo no está disponible.");
    }
}
