package co.edu.unbosque.biohub.interna.hallazgos.dominio;

// Una foto puede traer las coordenadas GPS en sus metadatos: solo la ve el autor o quien puede ver coordenadas
public class AccesoEvidenciaDenegadoException extends RuntimeException {

    public AccesoEvidenciaDenegadoException() {
        super("Solo el autor del hallazgo o el personal de alto rango pueden ver esta evidencia.");
    }
}
