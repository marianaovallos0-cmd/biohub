package co.edu.unbosque.biohub.interna.ubicaciones.infraestructura;

import co.edu.unbosque.biohub.interna.ubicaciones.dominio.RepositorioConsultaUbicaciones;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.UbicacionConsultada;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * El nombre sale de la versión vigente de la ficha. Las consultas públicas no piden
 * latitud ni longitud a PostgreSQL: las coordenadas no viajan si no hay permiso.
 */
@Repository
public class RepositorioConsultaUbicacionesJdbc implements RepositorioConsultaUbicaciones {

    private static final String DESDE = """
            FROM ubicacion u
            LEFT JOIN LATERAL (
                SELECT genero, especie FROM ficha_taxonomica f
                WHERE f.hallazgo_id = u.hallazgo_id
                ORDER BY f.numero_version DESC LIMIT 1
            ) f ON TRUE
            """;
    private static final String COLUMNAS_PUBLICAS =
            "SELECT u.hallazgo_id, f.genero, f.especie, u.region, u.departamento, u.especie_protegida ";
    private static final String COLUMNAS_EXACTAS = COLUMNAS_PUBLICAS + ", u.latitud, u.longitud ";
    private static final String ORDEN = " ORDER BY u.departamento, f.genero, f.especie";

    private final JdbcClient jdbc;

    public RepositorioConsultaUbicacionesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UbicacionConsultada> exacta(UUID hallazgoId) {
        return jdbc.sql(COLUMNAS_EXACTAS + DESDE + " WHERE u.hallazgo_id = :id")
                .param("id", hallazgoId).query((rs, n) -> exacta(rs)).optional();
    }

    @Override
    public Optional<UbicacionConsultada> publica(UUID hallazgoId) {
        return jdbc.sql(COLUMNAS_PUBLICAS + DESDE + " WHERE u.hallazgo_id = :id")
                .param("id", hallazgoId).query((rs, n) -> publica(rs)).optional();
    }

    @Override
    public List<UbicacionConsultada> todasExactas() {
        return jdbc.sql(COLUMNAS_EXACTAS + DESDE + ORDEN).query((rs, n) -> exacta(rs)).list();
    }

    @Override
    public List<UbicacionConsultada> todasPublicas() {
        return jdbc.sql(COLUMNAS_PUBLICAS + DESDE + ORDEN).query((rs, n) -> publica(rs)).list();
    }

    private static UbicacionConsultada exacta(ResultSet rs) throws SQLException {
        return UbicacionConsultada.exacta(rs.getObject("hallazgo_id", UUID.class), nombre(rs),
                rs.getString("region"), rs.getString("departamento"), rs.getBoolean("especie_protegida"),
                rs.getDouble("latitud"), rs.getDouble("longitud"));
    }

    private static UbicacionConsultada publica(ResultSet rs) throws SQLException {
        return UbicacionConsultada.publica(rs.getObject("hallazgo_id", UUID.class), nombre(rs),
                rs.getString("region"), rs.getString("departamento"), rs.getBoolean("especie_protegida"));
    }

    private static String nombre(ResultSet rs) throws SQLException {
        String genero = rs.getString("genero");
        String especie = rs.getString("especie");
        return ((genero == null ? "" : genero) + " " + (especie == null ? "" : especie)).trim();
    }
}
