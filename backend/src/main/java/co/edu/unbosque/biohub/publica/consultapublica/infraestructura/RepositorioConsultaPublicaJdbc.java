package co.edu.unbosque.biohub.publica.consultapublica.infraestructura;

import co.edu.unbosque.biohub.publica.consultapublica.dominio.RepositorioConsultaPublica;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.ResultadoBusqueda;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lee solo vista_publica_hallazgo con el usuario biohub_publica (solo lectura).
 * La vista ya trae la versión vigente de cada hallazgo y no tiene coordenadas.
 */
@Repository
public class RepositorioConsultaPublicaJdbc implements RepositorioConsultaPublica {

    private static final String SQL = """
            SELECT hallazgo_id, genero, especie, tipo_especie, clase, orden, familia,
                   region, departamento, fecha_registro
            FROM vista_publica_hallazgo
            WHERE lower(coalesce(genero, '') || ' ' || coalesce(especie, '')) LIKE :patron ESCAPE '\\'
            ORDER BY genero, especie, fecha_registro DESC
            LIMIT :limite
            """;

    private final JdbcClient jdbc;

    public RepositorioConsultaPublicaJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ResultadoBusqueda> buscarPorNombreEspecie(String texto, int limite) {
        return jdbc.sql(SQL)
                .param("patron", "%" + escaparLike(texto.toLowerCase()) + "%")
                .param("limite", limite)
                .query((rs, fila) -> new ResultadoBusqueda(
                        rs.getObject("hallazgo_id", UUID.class),
                        nombreCientifico(rs.getString("genero"), rs.getString("especie")),
                        rs.getString("tipo_especie"),
                        rs.getString("clase"),
                        rs.getString("orden"),
                        rs.getString("familia"),
                        rs.getString("region"),
                        rs.getString("departamento"),
                        rs.getObject("fecha_registro", OffsetDateTime.class)))
                .list();
    }

    private static String nombreCientifico(String genero, String especie) {
        return ((genero == null ? "" : genero) + " " + (especie == null ? "" : especie)).trim();
    }

    // Para que % y _ que escriba el usuario se busquen como texto y no como comodines.
    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
