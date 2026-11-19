package co.edu.unbosque.biohub.analitica.planificador;

import co.edu.unbosque.biohub.analitica.generador.aplicacion.GenerarReportePresion;
import co.edu.unbosque.biohub.analitica.generador.dominio.ConteoDepartamento;
import co.edu.unbosque.biohub.analitica.generador.dominio.IndicadorDepartamento;
import co.edu.unbosque.biohub.analitica.generador.dominio.ReportePendiente;
import co.edu.unbosque.biohub.analitica.generador.dominio.RepositorioAnalitico;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RevisarReportesPendientesTest {

    @Test
    void siUnReporteFallaLosDemasSeGeneranIgual() {
        ReportePendiente malo = new ReportePendiente(UUID.randomUUID(), YearMonth.of(2026, 9));
        ReportePendiente bueno = new ReportePendiente(UUID.randomUUID(), YearMonth.of(2026, 10));
        RepositorioFalso repositorio = new RepositorioFalso(List.of(malo, bueno), malo);
        RevisarReportesPendientes revisar = new RevisarReportesPendientes(repositorio,
                new GenerarReportePresion(repositorio, Clock.systemUTC()));

        revisar.revisar();

        assertThat(repositorio.generados).containsExactly(bueno.id());
    }

    // Doble tipo fake: falla al guardar el reporte "malo"
    private static class RepositorioFalso implements RepositorioAnalitico {

        private final List<ReportePendiente> pendientes;
        private final ReportePendiente queFalla;
        final List<UUID> generados = new ArrayList<>();

        RepositorioFalso(List<ReportePendiente> pendientes, ReportePendiente queFalla) {
            this.pendientes = pendientes;
            this.queFalla = queFalla;
        }

        @Override
        public List<ReportePendiente> pendientes() {
            return pendientes;
        }

        @Override
        public List<ConteoDepartamento> contar(YearMonth mes) {
            return List.of();
        }

        @Override
        public void guardar(UUID reporteId, List<IndicadorDepartamento> indicadores, OffsetDateTime generadoEn) {
            if (reporteId.equals(queFalla.id())) {
                throw new IllegalStateException("La base no respondió");
            }
            generados.add(reporteId);
        }
    }
}