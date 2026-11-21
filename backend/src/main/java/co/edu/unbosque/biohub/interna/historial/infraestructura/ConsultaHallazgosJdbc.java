package co.edu.unbosque.biohub.interna.historial.infraestructura;

import co.edu.unbosque.biohub.interna.historial.dominio.ConsultaHallazgos;
import co.edu.unbosque.biohub.interna.historial.dominio.ResumenHallazgo;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Una sola consulta en vez de una por hallazgo. La versión vigente es la de numero_version
// más alto: se calcula al leer, no se guarda (nota técnica de HU-06).
@Repository
public class ConsultaHallazgosJdbc implements ConsultaHallazgos {

    private static final String SQL = """
            SELECT h.id, h.fecha_registro, h.expedicion, u.nombre AS investigador,
                   te.nombre AS tipo_especie, f.genero, f.especie, f.familia, f.numero_version,
                   ub.departamento
            FROM hallazgo h
            JOIN LATERAL (
                SELECT * FROM ficha_taxonomica fv
                WHERE fv.hallazgo_id = h.id
                ORDER BY fv.numero_version DESC
                LIMIT 1
            ) f ON TRUE
            JOIN tipo_especie te ON te.id = f.tipo_especie_id
            JOIN usuario u       ON u.id = h.investigador_id
            LEFT JOIN ubicacion ub ON ub.hallazgo_id = h.id
            ORDER BY h.fecha_registro DESC
            """;

    private final JdbcClient jdbc;

    public ConsultaHallazgosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ResumenHallazgo> listarConVersionVigente() {
        return jdbc.sql(SQL)
                .query((rs, fila) -> new ResumenHallazgo(
                        rs.getObject("id", UUID.class),
                        rs.getObject("fecha_registro", OffsetDateTime.class),
                        rs.getString("expedicion"),
                        rs.getString("investigador"),
                        rs.getString("tipo_especie"),
                        (rs.getString("genero") + " " + rs.getString("especie")).trim(),
                        rs.getString("familia"),
                        rs.getString("departamento"),
                        rs.getInt("numero_version")))
                .list();
    }
}
