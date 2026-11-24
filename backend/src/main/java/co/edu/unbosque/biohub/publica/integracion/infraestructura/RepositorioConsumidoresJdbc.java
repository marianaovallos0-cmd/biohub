package co.edu.unbosque.biohub.publica.integracion.infraestructura;

import co.edu.unbosque.biohub.publica.integracion.dominio.ConsumidorExterno;
import co.edu.unbosque.biohub.publica.integracion.dominio.RepositorioConsumidores;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Lee consumidor_externo con el usuario biohub_publica, que solo tiene SELECT (V3__permisos.sql). */
@Repository
public class RepositorioConsumidoresJdbc implements RepositorioConsumidores {

    private static final String SQL = """
            SELECT id, organizacion, tipo, limite_por_minuto
            FROM consumidor_externo
            WHERE api_key = :apiKey
            """;

    private final JdbcClient jdbc;

    public RepositorioConsumidoresJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ConsumidorExterno> buscarPorApiKey(String apiKey) {
        return jdbc.sql(SQL)
                .param("apiKey", apiKey)
                .query((rs, fila) -> new ConsumidorExterno(
                        rs.getObject("id", UUID.class),
                        rs.getString("organizacion"),
                        rs.getString("tipo"),
                        rs.getInt("limite_por_minuto")))
                .optional();
    }
}
