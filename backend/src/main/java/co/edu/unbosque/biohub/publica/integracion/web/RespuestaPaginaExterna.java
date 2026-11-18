package co.edu.unbosque.biohub.publica.integracion.web;

import co.edu.unbosque.biohub.publica.integracion.dominio.HallazgoExterno;

import java.util.List;

/** Lo que devuelve GET /externo/hallazgos. */
public record RespuestaPaginaExterna(int pagina, int tamano, int total, List<HallazgoExterno> hallazgos) {

    static RespuestaPaginaExterna de(int pagina, int tamano, List<HallazgoExterno> hallazgos) {
        return new RespuestaPaginaExterna(pagina, tamano, hallazgos.size(), hallazgos);
    }
}
