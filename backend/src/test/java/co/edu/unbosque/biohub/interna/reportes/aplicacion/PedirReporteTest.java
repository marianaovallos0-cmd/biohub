package co.edu.unbosque.biohub.interna.reportes.aplicacion;

import co.edu.unbosque.biohub.interna.reportes.ReportesDePrueba;
import co.edu.unbosque.biohub.interna.reportes.dominio.MesInvalidoException;
import co.edu.unbosque.biohub.interna.reportes.dominio.Reporte;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedirReporteTest {

    // Hoy es 19 de noviembre de 2026
    private static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-11-19T15:00:00Z"), ZoneId.of("America/Bogota"));

    private final ReportesDePrueba reportes = new ReportesDePrueba();
    private final PedirReporte pedir = new PedirReporte(reportes, RELOJ);
    private final UsuarioAutenticado director = new UsuarioAutenticado(UUID.randomUUID(), "Jorge Peña",
            "director@biohub.test", NivelAutorizacion.DIRECTOR, Set.of("VER_REPORTES"));

    @Test
    void unMesTerminadoQuedaPendienteParaLaAnalitica() {
        Reporte reporte = pedir.ejecutar("2026-10", director);

        assertThat(reporte.mes()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(reporte.generado()).isFalse();
        assertThat(reportes.reportes).hasSize(1);
    }

    // Criterio 3: pedirlo otra vez devuelve el mismo, no crea otro
    @Test
    void siYaSePidioDevuelveElMismo() {
        Reporte primero = pedir.ejecutar("2026-10", director);

        Reporte segundo = pedir.ejecutar("2026-10", director);

        assertThat(segundo.id()).isEqualTo(primero.id());
        assertThat(reportes.reportes).hasSize(1);
    }

    // Valores límite: octubre sí; noviembre (el mes actual) y diciembre no
    @ParameterizedTest
    @ValueSource(strings = {"2026-11", "2026-12"})
    void noDejaPedirElMesActualNiUnoFuturo(String mes) {
        assertThatThrownBy(() -> pedir.ejecutar(mes, director))
                .isInstanceOf(MesInvalidoException.class)
                .hasMessage("Solo se pueden pedir reportes de meses que ya terminaron.");
        assertThat(reportes.reportes).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"octubre", "2026-13", "10-2026", ""})
    void rechazaUnMesConFormatoInvalido(String mes) {
        assertThatThrownBy(() -> pedir.ejecutar(mes, director))
                .isInstanceOf(MesInvalidoException.class)
                .hasMessageContaining("AAAA-MM");
        assertThat(reportes.reportes).isEmpty();
    }
}
