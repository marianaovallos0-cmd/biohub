package co.edu.unbosque.biohub.publica.consultapublica.web;

import co.edu.unbosque.biohub.publica.consultapublica.dominio.ResultadoBusqueda;

import java.util.List;

/** Lo que devuelve GET /publico/busqueda. */
public record RespuestaBusqueda(
        String consulta,
        int total,
        List<ResultadoBusqueda> resultados,
        String mensaje) {

    static RespuestaBusqueda de(String consulta, List<ResultadoBusqueda> resultados) {
        String mensaje = resultados.isEmpty()
                ? "No hay resultados para \"" + consulta + "\"."
                : null;
        return new RespuestaBusqueda(consulta, resultados.size(), resultados, mensaje);
    }
}
