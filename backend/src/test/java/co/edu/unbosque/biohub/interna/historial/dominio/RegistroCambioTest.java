package co.edu.unbosque.biohub.interna.historial.dominio;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.JUSTIFICACION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// HU-07
class RegistroCambioTest {

    private final OffsetDateTime ahora = OffsetDateTime.now();
    private final UUID auditor = UUID.randomUUID();
    private final UUID anterior = UUID.randomUUID();
    private final UUID nueva = UUID.randomUUID();

    @Test
    void guardaFechaJustificacionYQuienAutorizo() {
        RegistroCambio cambio = new RegistroCambio(ahora, "  " + JUSTIFICACION + "  ", auditor, anterior, nueva);

        assertThat(cambio.getFechaHora()).isEqualTo(ahora);
        assertThat(cambio.getJustificacion()).isEqualTo(JUSTIFICACION);
        assertThat(cambio.getAutorizadoPor()).isEqualTo(auditor);
        assertThat(cambio.getVersionAnteriorId()).isEqualTo(anterior);
        assertThat(cambio.getVersionNuevaId()).isEqualTo(nueva);
    }

    @Test
    void sinJustificacionNoSeCrea() {
        assertThatThrownBy(() -> new RegistroCambio(ahora, "   ", auditor, anterior, nueva))
                .isInstanceOf(CambioInvalidoException.class)
                .extracting(e -> ((CambioInvalidoException) e).getErrores())
                .asList().containsExactly("Falta la justificación del cambio.");
        assertThatThrownBy(() -> new RegistroCambio(ahora, null, auditor, anterior, nueva))
                .isInstanceOf(CambioInvalidoException.class);
    }

    @Test
    void unaJustificacionDemasiadoCortaNoSirve() {
        assertThat(RegistroCambio.errores("porque sí", auditor))
                .containsExactly("La justificación debe tener al menos 10 caracteres.");
    }

    @Test
    void sinAutorizadorNoSeCrea() {
        assertThatThrownBy(() -> new RegistroCambio(ahora, JUSTIFICACION, null, anterior, nueva))
                .isInstanceOf(CambioInvalidoException.class)
                .extracting(e -> ((CambioInvalidoException) e).getErrores())
                .asList().containsExactly("Falta el usuario que autoriza el cambio.");
    }

    @Test
    void necesitaDosVersionesDistintas() {
        assertThatThrownBy(() -> new RegistroCambio(ahora, JUSTIFICACION, auditor, anterior, anterior))
                .isInstanceOf(CambioInvalidoException.class);
        assertThatThrownBy(() -> new RegistroCambio(ahora, JUSTIFICACION, auditor, null, nueva))
                .isInstanceOf(CambioInvalidoException.class);
    }

    @Test
    void noTieneMetodosParaModificarlo() {
        Method[] publicos = Arrays.stream(RegistroCambio.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()))
                .toArray(Method[]::new);

        assertThat(publicos).allSatisfy(m -> assertThat(m.getName()).startsWith("get"));
    }
}
