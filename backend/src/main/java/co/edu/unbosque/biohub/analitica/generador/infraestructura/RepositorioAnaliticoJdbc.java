package co.edu.unbosque.biohub.analitica.generador.infraestructura;

import co.edu.unbosque.biohub.analitica.generador.dominio.ConteoDepartamento;
import co.edu.unbosque.biohub.analitica.generador.dominio.IndicadorDepartamento;
import co.edu.unbosque.biohub.analitica.generador.dominio.ProgramadorReportes;
import co.edu.unbosque.biohub.analitica.generador.dominio.ReportePendiente;
import co.edu.unbosque.biohub.analitica.generador.dominio.RepositorioAnalitico;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

// SQL directo y no JPA: la analítica no tiene permiso de leer latitud ni longitud (V3__permisos, ASR-1)
@Repository
public class RepositorioAnaliticoJdbc implements RepositorioAnalitico, ProgramadorReportes {

    public static final String TIPO = "PRESION_ECOLOGICA";
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final JdbcClient jdbc;

    public RepositorioAnaliticoJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ReportePendiente> pendientes() {
        return jdbc.sql("SELECT id, parametros FROM reporte WHERE tipo = :tipo AND fecha_generacion IS NULL")
                .param("tipo", TIPO)
                .query((fila, n) -> new ReportePendiente(fila.getObject("id", UUID.class),
                        YearMonth.parse(fila.getString("parametros"))))
                .list();
    }

    @Override
    public List<ConteoDepartamento> contar(YearMonth mes) {
        return jdbc.sql("""
                        SELECT u.departamento,
                               count(*) AS total,
                               count(*) FILTER (WHERE u.especie_protegida) AS protegidos
                        FROM hallazgo h
                        JOIN ubicacion u ON u.hallazgo_id = h.id
                        WHERE h.fecha_registro >= :desde AND h.fecha_registro < :hasta
                        GROUP BY u.departamento
                        """)
                .param("desde", inicio(mes))
                .param("hasta", inicio(mes.plusMonths(1)))
                .query((fila, n) -> new ConteoDepartamento(fila.getString("departamento"),
                        fila.getInt("total"), fila.getInt("protegidos")))
                .list();
    }

    @Override
    public void guardar(UUID reporteId, List<IndicadorDepartamento> indicadores, OffsetDateTime generadoEn) {
        for (IndicadorDepartamento indicador : indicadores) {
            jdbc.sql("""
                            INSERT INTO reporte_presion (reporte_id, departamento, total_hallazgos,
                                                         hallazgos_protegidos, variacion_porcentual, en_alerta)
                            VALUES (:reporte, :departamento, :total, :protegidos, :variacion, :alerta)
                            """)
                    .param("reporte", reporteId)
                    .param("departamento", indicador.departamento())
                    .param("total", indicador.totalHallazgos())
                    .param("protegidos", indicador.hallazgosProtegidos())
                    .param("variacion", indicador.variacionPorcentual())
                    .param("alerta", indicador.enAlerta())
                    .update();
        }
        jdbc.sql("UPDATE reporte SET fecha_generacion = :fecha WHERE id = :id")
                .param("fecha", generadoEn)
                .param("id", reporteId)
                .update();
    }

    // HU-19: el reporte automático no lo pide nadie, por eso solicitado_por queda vacío
    @Override
    public boolean programar(YearMonth mes) {
        return jdbc.sql("""
                        INSERT INTO reporte (tipo, parametros)
                        VALUES (:tipo, :mes)
                        ON CONFLICT (tipo, parametros) DO NOTHING
                        """)
                .param("tipo", TIPO)
                .param("mes", mes.toString())
                .update() == 1;
    }

    // El mes empieza a las 00:00 en Colombia, no en UTC
    private static OffsetDateTime inicio(YearMonth mes) {
        return mes.atDay(1).atStartOfDay(BOGOTA).toOffsetDateTime();
    }
}