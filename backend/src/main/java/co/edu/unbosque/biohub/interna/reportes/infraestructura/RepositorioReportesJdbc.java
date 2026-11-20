package co.edu.unbosque.biohub.interna.reportes.infraestructura;

import co.edu.unbosque.biohub.interna.reportes.dominio.FilaReporte;
import co.edu.unbosque.biohub.interna.reportes.dominio.Reporte;
import co.edu.unbosque.biohub.interna.reportes.dominio.RepositorioReportes;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Solo lee y pide reportes. El cálculo lo hace el contenedor analítica (ASR-6)
@Repository
public class RepositorioReportesJdbc implements RepositorioReportes {

    // El mismo tipo que usa la analítica en RepositorioAnaliticoJdbc
    static final String TIPO = "PRESION_ECOLOGICA";
    private static final String SELECT = "SELECT id, parametros, fecha_generacion FROM reporte WHERE tipo = :tipo";

    private final JdbcClient jdbc;

    public RepositorioReportesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Reporte> buscarPorMes(YearMonth mes) {
        return jdbc.sql(SELECT + " AND parametros = :mes")
                .param("tipo", TIPO)
                .param("mes", mes.toString())
                .query(RepositorioReportesJdbc::aReporte)
                .optional();
    }

    @Override
    public Optional<Reporte> buscarPorId(UUID id) {
        return jdbc.sql(SELECT + " AND id = :id")
                .param("tipo", TIPO)
                .param("id", id)
                .query(RepositorioReportesJdbc::aReporte)
                .optional();
    }

    @Override
    public List<Reporte> listar() {
        return jdbc.sql(SELECT + " ORDER BY parametros DESC")
                .param("tipo", TIPO)
                .query(RepositorioReportesJdbc::aReporte)
                .list();
    }

    @Override
    public Reporte crearPendiente(YearMonth mes, UUID solicitadoPor) {
        // Si dos peticiones llegan al tiempo, la regla UNIQUE de la V4 deja una sola y la otra no hace nada
        jdbc.sql("""
                        INSERT INTO reporte (tipo, parametros, solicitado_por)
                        VALUES (:tipo, :mes, :usuario)
                        ON CONFLICT (tipo, parametros) DO NOTHING
                        """)
                .param("tipo", TIPO)
                .param("mes", mes.toString())
                .param("usuario", solicitadoPor)
                .update();
        return buscarPorMes(mes).orElseThrow();
    }

    @Override
    public List<FilaReporte> filas(UUID reporteId) {
        return jdbc.sql("""
                        SELECT departamento, total_hallazgos, hallazgos_protegidos, variacion_porcentual, en_alerta
                        FROM reporte_presion
                        WHERE reporte_id = :id
                        ORDER BY departamento
                        """)
                .param("id", reporteId)
                .query((fila, n) -> new FilaReporte(fila.getString("departamento"),
                        fila.getInt("total_hallazgos"), fila.getInt("hallazgos_protegidos"),
                        fila.getBigDecimal("variacion_porcentual"), fila.getBoolean("en_alerta")))
                .list();
    }

    private static Reporte aReporte(ResultSet fila, int n) throws SQLException {
        return new Reporte(fila.getObject("id", UUID.class), YearMonth.parse(fila.getString("parametros")),
                fila.getObject("fecha_generacion", OffsetDateTime.class));
    }
}
