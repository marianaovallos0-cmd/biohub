package co.edu.unbosque.biohub.analitica.generador.aplicacion;

import co.edu.unbosque.biohub.analitica.generador.dominio.ConteoDepartamento;
import co.edu.unbosque.biohub.analitica.generador.dominio.IndicadorDepartamento;
import co.edu.unbosque.biohub.analitica.generador.dominio.ReportePendiente;
import co.edu.unbosque.biohub.analitica.generador.dominio.RepositorioAnalitico;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GenerarReportePresionTest {

    private static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-11-01T06:00:00Z"), ZoneId.of("America/Bogota"));
    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

    private final RepositorioFalso repositorio = new RepositorioFalso();
    private final GenerarReportePresion generar = new GenerarReportePresion(repositorio, RELOJ);
    private final ReportePendiente pendiente = new ReportePendiente(UUID.randomUUID(), OCTUBRE);

    // Criterio 1: el reporte del mes trae los indicadores por departamento
    @Test
    void comparaCadaDepartamentoConElMesAnterior() {
        repositorio.conteos.put(OCTUBRE, List.of(
                new ConteoDepartamento("Chocó", 7, 2),
                new ConteoDepartamento("Antioquia", 3, 0)));
        repositorio.conteos.put(OCTUBRE.minusMonths(1), List.of(
                new ConteoDepartamento("Chocó", 10, 4),
                new ConteoDepartamento("Caldas", 4, 1)));

        List<IndicadorDepartamento> filas = generar.generar(pendiente);

        assertThat(filas).extracting(IndicadorDepartamento::departamento)
                .containsExactly("Antioquia", "Caldas", "Chocó");
        // Antioquia no tenía hallazgos en septiembre: sin variación
        assertThat(filas.get(0).variacionPorcentual()).isNull();
        // Caldas bajó a cero: −100 % y en alerta
        assertThat(filas.get(1).totalHallazgos()).isZero();
        assertThat(filas.get(1).enAlerta()).isTrue();
        // Chocó pasó de 10 a 7: −30 % y en alerta
        assertThat(filas.get(2).hallazgosProtegidos()).isEqualTo(2);
        assertThat(filas.get(2).enAlerta()).isTrue();

        assertThat(repositorio.guardadoId).isEqualTo(pendiente.id());
        assertThat(repositorio.generadoEn).isEqualTo(OffsetDateTime.now(RELOJ));
    }

    // Criterio 2: un mes sin registros se genera en cero, sin error
    @Test
    void unMesSinHallazgosSeGeneraIgualEnCero() {
        List<IndicadorDepartamento> filas = generar.generar(pendiente);

        assertThat(filas).isEmpty();
        assertThat(repositorio.guardadoId).isEqualTo(pendiente.id());
        assertThat(repositorio.generadoEn).isNotNull();
    }

    // Doble tipo fake: guarda en memoria en vez de PostgreSQL
    private static class RepositorioFalso implements RepositorioAnalitico {

        final Map<YearMonth, List<ConteoDepartamento>> conteos = new HashMap<>();
        UUID guardadoId;
        List<IndicadorDepartamento> guardados;
        OffsetDateTime generadoEn;

        @Override
        public List<ReportePendiente> pendientes() {
            return List.of();
        }

        @Override
        public List<ConteoDepartamento> contar(YearMonth mes) {
            return conteos.getOrDefault(mes, List.of());
        }

        @Override
        public void guardar(UUID reporteId, List<IndicadorDepartamento> indicadores, OffsetDateTime generadoEn) {
            this.guardadoId = reporteId;
            this.guardados = indicadores;
            this.generadoEn = generadoEn;
        }
    }
}