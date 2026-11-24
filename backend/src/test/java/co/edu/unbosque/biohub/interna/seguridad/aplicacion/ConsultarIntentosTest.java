package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.RepositorioIntentosFalso;
import co.edu.unbosque.biohub.interna.seguridad.dominio.IntentoAccesoNoAutorizado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RangoFechasInvalidoException;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.rol;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.usuario;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.INVESTIGADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarIntentosTest {

    private static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneId.of("America/Bogota"));

    private final RepositorioIntentosFalso intentos = new RepositorioIntentosFalso();
    private final ConsultarIntentos consultar = new ConsultarIntentos(intentos, RELOJ);
    private final Usuario laura =
            usuario("Laura Gómez", "investigador1@biohub.test", rol(INVESTIGADOR, "REGISTRAR_HALLAZGO"));

    private void intentoEl(String fechaHora) {
        intentos.guardar(new IntentoAccesoNoAutorizado(laura, "GET /interno/intentos", "10.0.0.7",
                OffsetDateTime.parse(fechaHora)));
    }

    // Valores límite: justo antes, justo al inicio, al final del último día y justo después
    @Test
    void incluyeElPrimerYElUltimoDiaCompletos() {
        intentoEl("2026-11-13T23:59-05:00");
        intentoEl("2026-11-14T00:00-05:00");
        intentoEl("2026-11-15T23:30-05:00");
        intentoEl("2026-11-16T00:00-05:00");

        List<IntentoRegistrado> resultado = consultar.ejecutar(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 15));

        assertThat(resultado).extracting(IntentoRegistrado::fechaHora).containsExactly(
                OffsetDateTime.parse("2026-11-15T23:30-05:00"),
                OffsetDateTime.parse("2026-11-14T00:00-05:00"));
    }

    @Test
    void sePuedeConsultarUnSoloDia() {
        intentoEl("2026-11-14T10:00-05:00");
        intentoEl("2026-11-15T10:00-05:00");

        assertThat(consultar.ejecutar(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 15))).hasSize(1);
    }

    @Test
    void muestraQuienLoIntentoYQuePidio() {
        intentoEl("2026-11-15T10:00-05:00");

        IntentoRegistrado intento = consultar.ejecutar(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 15)).get(0);

        assertThat(intento.correo()).isEqualTo("investigador1@biohub.test");
        assertThat(intento.rol()).isEqualTo("INVESTIGADOR");
        assertThat(intento.recurso()).isEqualTo("GET /interno/intentos");
        assertThat(intento.ip()).isEqualTo("10.0.0.7");
    }

    @Test
    void rechazaUnRangoAlReves() {
        assertThatThrownBy(() -> consultar.ejecutar(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(RangoFechasInvalidoException.class);
    }
}
