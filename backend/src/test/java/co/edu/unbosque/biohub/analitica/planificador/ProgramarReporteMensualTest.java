package co.edu.unbosque.biohub.analitica.planificador;

import co.edu.unbosque.biohub.analitica.generador.aplicacion.GenerarReportePresion;
import co.edu.unbosque.biohub.analitica.generador.dominio.ConteoDepartamento;
import co.edu.unbosque.biohub.analitica.generador.dominio.IndicadorDepartamento;
import co.edu.unbosque.biohub.analitica.generador.dominio.ProgramadorReportes;
import co.edu.unbosque.biohub.analitica.generador.dominio.ReportePendiente;
import co.edu.unbosque.biohub.analitica.generador.dominio.RepositorioAnalitico;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProgramarReporteMensualTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    /** Doble tipo fake de la base: guarda los reportes pedidos y los que se generan. */
    static class BaseFalsa implements RepositorioAnalitico, ProgramadorReportes {
        final Map<YearMonth, UUID> pedidos = new LinkedHashMap<>();
        final List<YearMonth> generados = new ArrayList<>();

        @Override
        public boolean programar(YearMonth mes) {
            return pedidos.putIfAbsent(mes, UUID.randomUUID()) == null;
        }

        @Override
        public List<ReportePendiente> pendientes() {
            return pedidos.entrySet().stream()
                    .filter(e -> !generados.contains(e.getKey()))
                    .map(e -> new ReportePendiente(e.getValue(), e.getKey()))
                    .toList();
        }

        @Override
        public List<ConteoDepartamento> contar(YearMonth mes) {
            return List.of(new ConteoDepartamento("Chocó", 4, 3));
        }

        @Override
        public void guardar(UUID reporteId, List<IndicadorDepartamento> indicadores, OffsetDateTime generadoEn) {
            pedidos.forEach((mes, id) -> {
                if (id.equals(reporteId)) {
                    generados.add(mes);
                }
            });
        }
    }

    private static ProgramarReporteMensual planificador(BaseFalsa base, String ahora) {
        Clock reloj = Clock.fixed(ZonedDateTime.parse(ahora).toInstant(), BOGOTA);
        RevisarReportesPendientes revisar = new RevisarReportesPendientes(base, new GenerarReportePresion(base, reloj));
        return new ProgramarReporteMensual(base, revisar, reloj);
    }

    @Test
    void elDiaUnoCalculaElMesAnterior() {
        BaseFalsa base = new BaseFalsa();

        planificador(base, "2026-12-01T02:00-05:00[America/Bogota]").ejecutar();

        assertThat(base.pedidos).containsOnlyKeys(YearMonth.of(2026, 11));
        assertThat(base.generados).containsExactly(YearMonth.of(2026, 11));
    }

    @Test
    void enEneroCalculaDiciembreDelAnoAnterior() {
        BaseFalsa base = new BaseFalsa();

        planificador(base, "2027-01-01T02:00-05:00[America/Bogota]").ejecutar();

        assertThat(base.generados).containsExactly(YearMonth.of(2026, 12));
    }

    @Test
    void siElDirectorYaLoHabiaPedidoNoSeDuplica() {
        BaseFalsa base = new BaseFalsa();
        base.programar(YearMonth.of(2026, 11));
        UUID delDirector = base.pedidos.get(YearMonth.of(2026, 11));

        planificador(base, "2026-12-01T02:00-05:00[America/Bogota]").ejecutar();

        assertThat(base.pedidos).hasSize(1).containsEntry(YearMonth.of(2026, 11), delDirector);
        assertThat(base.generados).containsExactly(YearMonth.of(2026, 11));
    }

    @Test
    void tambienCalculaLosPendientesQueHabiaPedidoElDirector() {
        BaseFalsa base = new BaseFalsa();
        base.programar(YearMonth.of(2026, 9));

        planificador(base, "2026-12-01T02:00-05:00[America/Bogota]").ejecutar();

        assertThat(base.generados).containsExactlyInAnyOrder(YearMonth.of(2026, 9), YearMonth.of(2026, 11));
    }

    @Test
    void laTareaQuedaProgramadaElDiaUnoALasDosDeLaMananaEnHoraDeColombia() throws Exception {
        Scheduled programacion = ProgramarReporteMensual.class.getMethod("ejecutar").getAnnotation(Scheduled.class);
        assertThat(programacion.zone()).isEqualTo("America/Bogota");

        // El valor por defecto que queda si no se cambia BIOHUB_ANALITICA_CRON_MENSUAL
        String cron = programacion.cron().replaceAll("^\\$\\{[^:]+:(.*)}$", "$1");
        ZonedDateTime siguiente = CronExpression.parse(cron)
                .next(ZonedDateTime.parse("2026-11-23T14:00-05:00[America/Bogota]"));

        assertThat(siguiente).isEqualTo(ZonedDateTime.parse("2026-12-01T02:00-05:00[America/Bogota]"));
    }
}
