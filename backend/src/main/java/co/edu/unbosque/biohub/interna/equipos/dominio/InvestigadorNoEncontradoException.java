package co.edu.unbosque.biohub.interna.equipos.dominio;

public class InvestigadorNoEncontradoException extends RuntimeException {

    public InvestigadorNoEncontradoException() {
        super("El investigador no existe o está inactivo.");
    }
}
