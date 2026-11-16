package co.edu.unbosque.biohub.publica.integracion.infraestructura;

import co.edu.unbosque.biohub.publica.integracion.dominio.HallazgoExterno;
import co.edu.unbosque.biohub.publica.integracion.dominio.RepositorioDatosExternos;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lee solo vista_publica_hallazgo, la misma de la consulta pública.
 * La vista no tiene latitud ni longitud, así que a los externos tampoco les llegan.
 */
@Repository
public class RepositorioDatosExternosJdbc implements RepositorioDatosExternos {

    private static final String COLUMNAS = """
            SELECT hallazgo_id, fecha_registro, tipo_especie,
                   reino, filo, clase, orden, familia, genero, especie,
                   region, departamento
            FROM vista_publica_hallazgo
            """;

    private static final RowMapper<HallazgoExterno> MAPEO = (rs, fila) -> {
        String genero = rs.getString("genero");
        String especie = rs.getString("especie");
        return new HallazgoExterno(
                rs.getObject("hallazgo_id", UUID.class),
                ((genero == null ? "" : genero) + " " + (especie == null ? "" : especie)).trim(),
                rs.getString("tipo_especie"),
                rs.getString("reino"),
                rs.getString("filo"),
                rs.getString("clase"),
                rs.getString("orden"),
                rs.getString("familia"),
                genero,
                especie,
                rs.getString("region"),
                rs.getString("departamento"),
                rs.getObject("fecha_registro", OffsetDateTime.class));
    };

    private final JdbcClient jdbc;

    public RepositorioDatosExternosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<HallazgoExterno> listar(int desde, int cantidad) {
        // Orden fijo para que las páginas no se repitan ni se salten registros
        return jdbc.sql(COLUMNAS + " ORDER BY fecha_registro, hallazgo_id LIMIT :cantidad OFFSET :desde")
                .param("cantidad", cantidad)
                .param("desde", desde)
                .query(MAPEO)
                .list();
    }

    @Override
    public Optional<HallazgoExterno> buscarPorId(UUID hallazgoId) {
        return jdbc.sql(COLUMNAS + " WHERE hallazgo_id = :id")
                .param("id", hallazgoId)
                .query(MAPEO)
                .optional();
    }
}
