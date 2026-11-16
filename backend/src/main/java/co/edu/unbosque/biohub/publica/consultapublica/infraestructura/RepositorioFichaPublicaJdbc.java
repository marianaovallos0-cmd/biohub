package co.edu.unbosque.biohub.publica.consultapublica.infraestructura;

import co.edu.unbosque.biohub.publica.consultapublica.dominio.FichaPublica;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.RepositorioFichaPublica;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.UbicacionPublica;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Lee solo vista_publica_hallazgo con el usuario biohub_publica (solo lectura).
 * La vista ya trae la versión vigente del hallazgo y no tiene coordenadas.
 */
@Repository
public class RepositorioFichaPublicaJdbc implements RepositorioFichaPublica {

    private static final String SQL = """
            SELECT hallazgo_id, fecha_registro, tipo_especie,
                   reino, filo, clase, orden, familia, genero, especie,
                   region, departamento
            FROM vista_publica_hallazgo
            WHERE hallazgo_id = :id
            """;

    private final JdbcClient jdbc;

    public RepositorioFichaPublicaJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<FichaPublica> buscarPorHallazgo(UUID hallazgoId) {
        return jdbc.sql(SQL)
                .param("id", hallazgoId)
                .query((rs, fila) -> {
                    String genero = rs.getString("genero");
                    String especie = rs.getString("especie");
                    String departamento = rs.getString("departamento");
                    // Sin fila en ubicacion, el LEFT JOIN de la vista deja región y departamento en null
                    UbicacionPublica ubicacion = departamento == null
                            ? null
                            : new UbicacionPublica(rs.getString("region"), departamento);
                    return new FichaPublica(
                            rs.getObject("hallazgo_id", UUID.class),
                            nombreCientifico(genero, especie),
                            rs.getString("tipo_especie"),
                            rs.getString("reino"),
                            rs.getString("filo"),
                            rs.getString("clase"),
                            rs.getString("orden"),
                            rs.getString("familia"),
                            genero,
                            especie,
                            rs.getObject("fecha_registro", OffsetDateTime.class),
                            ubicacion);
                })
                .optional();
    }

    private static String nombreCientifico(String genero, String especie) {
        return ((genero == null ? "" : genero) + " " + (especie == null ? "" : especie)).trim();
    }
}
