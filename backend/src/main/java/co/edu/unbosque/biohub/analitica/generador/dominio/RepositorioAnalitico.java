package co.edu.unbosque.biohub.analitica.generador.dominio;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface RepositorioAnalitico {

    // Reportes que el director pidió y todavía no se han calculado
    List<ReportePendiente> pendientes();

    // Hallazgos por departamento registrados en ese mes
    List<ConteoDepartamento> contar(YearMonth mes);

    // Guarda las filas del reporte y lo marca como generado
    void guardar(UUID reporteId, List<IndicadorDepartamento> indicadores, OffsetDateTime generadoEn);
}