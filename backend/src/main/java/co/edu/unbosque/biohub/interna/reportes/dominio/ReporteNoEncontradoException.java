package co.edu.unbosque.biohub.interna.reportes.dominio;

public class ReporteNoEncontradoException extends RuntimeException {

    public ReporteNoEncontradoException() {
        super("El reporte no existe.");
    }
}
