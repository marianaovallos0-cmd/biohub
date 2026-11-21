package co.edu.unbosque.biohub.interna.hallazgos.dominio;

public class HallazgoNoEncontradoException extends RuntimeException {

    public HallazgoNoEncontradoException() {
        super("El hallazgo no existe.");
    }
}
