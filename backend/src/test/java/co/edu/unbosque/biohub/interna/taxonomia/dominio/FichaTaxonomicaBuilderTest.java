package co.edu.unbosque.biohub.interna.taxonomia.dominio;

import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.idDe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;

// HU-02 en el dominio: Builder + Strategy, sin Spring ni base de datos
class FichaTaxonomicaBuilderTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-13T10:00-05:00");

    private final TipoEspecie anfibio = TaxonomiaDePrueba.anfibio();
    private final TipoEspecie ave = TaxonomiaDePrueba.ave();
    private final UUID hallazgoId = UUID.randomUUID();

    private FichaTaxonomica.Builder ranaBase() {
        return FichaTaxonomica.builder(anfibio)
                .paraHallazgo(hallazgoId).fechaVersion(AHORA)
                .reino("Animalia").filo("Chordata").clase("Amphibia").orden("Anura")
                .familia("Dendrobatidae").genero("Oophaga").especie("histrionica");
    }

    private static List<String> erroresAl(FichaTaxonomica.Builder builder) {
        return assertThrows(FichaInvalidaException.class, builder::build).getErrores();
    }

    @Test
    void armaLaFichaConLosAtributosDeSuTipo() {
        FichaTaxonomica ficha = ranaBase()
                .atributo(idDe(anfibio, "temperatura_agua_c"), "24.5")
                .atributo(idDe(anfibio, "grabacion_acustica"), "evidencias/canto.wav")
                .build();

        assertThat(ficha.getNumeroVersion()).isEqualTo(1);
        assertThat(ficha.getHallazgoId()).isEqualTo(hallazgoId);
        assertThat(ficha.validarObligatorios()).isTrue();
        assertThat(ficha.getValores()).extracting(v -> v.getDefinicion().getNombre(), ValorAtributo::getValor)
                .containsExactly(
                        tuple("temperatura_agua_c", "24.5"),
                        tuple("grabacion_acustica", "evidencias/canto.wav"));
    }

    @Test
    void sinUnAtributoObligatorioNoSeArmaYDiceCualFalta() {
        assertThat(erroresAl(ranaBase()))
                .containsExactly("Falta el atributo obligatorio «temperatura_agua_c».");
    }

    @Test
    void unValorVacioCuentaComoFaltante() {
        assertThat(erroresAl(ranaBase().atributo(idDe(anfibio, "temperatura_agua_c"), "   ")))
                .containsExactly("Falta el atributo obligatorio «temperatura_agua_c».");
    }

    @Test
    void noAceptaUnValorDeOtroTipoDeDato() {
        assertThat(erroresAl(ranaBase().atributo(idDe(anfibio, "temperatura_agua_c"), "tibia")))
                .containsExactly(
                        "El atributo «temperatura_agua_c» debe ser un número (use punto para los decimales).");
    }

    @Test
    void cadaTipoConservaSusAtributosSinHeredarLosDeOtro() {
        // ruta_gps es del ave: en una rana no se acepta
        List<String> errores = erroresAl(ranaBase()
                .atributo(idDe(anfibio, "temperatura_agua_c"), "24.5")
                .atributo(idDe(ave, "ruta_gps"), "rutas/r1.gpx"));

        assertThat(errores).hasSize(1);
        assertThat(errores.get(0)).contains("no pertenece al tipo «Anfibio»");
    }

    @Test
    void elAveNoTieneQueLlenarLosAtributosDelAnfibio() {
        FichaTaxonomica ficha = FichaTaxonomica.builder(ave)
                .paraHallazgo(hallazgoId).fechaVersion(AHORA)
                .genero("Ara").especie("macao")
                .atributo(idDe(ave, "ruta_gps"), "rutas/amazonas-005.gpx")
                .build();

        assertThat(ficha.validarObligatorios()).isTrue();
        assertThat(ficha.getValores()).hasSize(1);
    }

    @Test
    void reuneTodosLosErroresEnUnaSolaRespuesta() {
        List<String> errores = erroresAl(FichaTaxonomica.builder(anfibio)
                .paraHallazgo(hallazgoId).fechaVersion(AHORA)
                .atributo(idDe(anfibio, "longitud_hocico_cloaca_mm"), "larga"));

        assertThat(errores).containsExactly(
                "Falta el género.",
                "Falta la especie.",
                "Falta el atributo obligatorio «temperatura_agua_c».",
                "El atributo «longitud_hocico_cloaca_mm» debe ser un número (use punto para los decimales).");
    }

    @Test
    void laVersionEmpiezaEnUno() {
        assertThatThrownBy(() -> ranaBase().version(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sinHallazgoEsUnErrorDeProgramacion() {
        assertThatThrownBy(() -> FichaTaxonomica.builder(anfibio).fechaVersion(AHORA)
                .genero("Oophaga").especie("histrionica").build())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void laFichaNoSePuedeModificarDespues() {
        FichaTaxonomica ficha = ranaBase().atributo(idDe(anfibio, "temperatura_agua_c"), "24.5").build();

        assertThatThrownBy(() -> ficha.getValores().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
