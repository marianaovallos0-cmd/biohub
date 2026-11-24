package co.edu.unbosque.biohub.interna.reportes.aplicacion;

import co.edu.unbosque.biohub.interna.reportes.ReportesDePrueba;
import co.edu.unbosque.biohub.interna.reportes.dominio.FilaReporte;
import co.edu.unbosque.biohub.interna.reportes.dominio.Reporte;
import co.edu.unbosque.biohub.interna.reportes.dominio.ReporteNoEncontradoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class ConsultarReportesTest {

    private static final OffsetDateTime GENERADO = OffsetDateTime.parse("2026-11-01T01:00-05:00");

    private final ReportesDePrueba reportes = new ReportesDePrueba();
    private final ConsultarReportes consultar = new ConsultarReportes(reportes);

    private Reporte generado(YearMonth mes) {
        Reporte reporte = new Reporte(UUID.randomUUID(), mes, GENERADO);
        reportes.reportes.add(reporte);
        return reporte;
    }

    // Criterio 1: los indicadores del mes con sus totales
    @Test
    void elDetalleSumaLosTotalesYCuentaLasAlertas() {
        Reporte octubre = generado(YearMonth.of(2026, 10));
        reportes.filas.put(octubre.id(), List.of(
                new FilaReporte("Amazonas", 2, 0, new BigDecimal("100.00"), false),
                new FilaReporte("Chocó", 0, 0, new BigDecimal("-100.00"), true),
                new FilaReporte("Magdalena", 1, 1, null, false)));

        DetalleReporte detalle = consultar.detalle(octubre.id());

        assertThat(detalle.mes()).isEqualTo("2026-10");
        assertThat(detalle.generado()).isTrue();
        assertThat(detalle.totalHallazgos()).isEqualTo(3);
        assertThat(detalle.hallazgosProtegidos()).isEqualTo(1);
        assertThat(detalle.departamentosEnAlerta()).isEqualTo(1);
        assertThat(detalle.filas()).hasSize(3);
    }

    // Criterio 2: sin registros, totales en cero y no un error
    @Test
    void unMesSinRegistrosMuestraTotalesEnCero() {
        Reporte vacio = generado(YearMonth.of(2026, 7));

        DetalleReporte detalle = consultar.detalle(vacio.id());

        assertThat(detalle.totalHallazgos()).isZero();
        assertThat(detalle.hallazgosProtegidos()).isZero();
        assertThat(detalle.departamentosEnAlerta()).isZero();
        assertThat(detalle.filas()).isEmpty();
    }

    @Test
    void laListaDiceCualesYaEstanGenerados() {
        generado(YearMonth.of(2026, 10));
        reportes.crearPendiente(YearMonth.of(2026, 9), UUID.randomUUID());

        assertThat(consultar.listar()).extracting(ResumenReporte::mes, ResumenReporte::generado)
                .containsExactly(
                        tuple("2026-10", true),
                        tuple("2026-09", false));
    }

    @Test
    void unReporteQueNoExisteSeAvisa() {
        assertThatThrownBy(() -> consultar.detalle(UUID.randomUUID()))
                .isInstanceOf(ReporteNoEncontradoException.class);
    }
}
