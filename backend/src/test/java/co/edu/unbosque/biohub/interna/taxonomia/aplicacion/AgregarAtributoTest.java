package co.edu.unbosque.biohub.interna.taxonomia.aplicacion;

import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.RepositorioFichasFalso;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.RepositorioTaxonomiaFalso;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.AtributoInvalidoException;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaInvalidaException;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaTaxonomica;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoDato;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecieNoEncontradoException;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.ValorAtributo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.idDe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

// HU-03: agregar un atributo con el sistema corriendo, sin tocar el código ni las fichas anteriores
class AgregarAtributoTest {

    private static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-11-19T15:00:00Z"), ZoneId.of("America/Bogota"));

    private final RepositorioTaxonomiaFalso taxonomia = new RepositorioTaxonomiaFalso();
    private final RepositorioFichasFalso fichas = new RepositorioFichasFalso();
    private final AgregarAtributo agregar = new AgregarAtributo(taxonomia);
    private final ServicioFichas servicioFichas = new ServicioFichas(taxonomia, fichas, RELOJ);
    private TipoEspecie anfibio;
    private TipoEspecie ave;

    @BeforeEach
    void preparar() {
        anfibio = TaxonomiaDePrueba.anfibio();
        ave = TaxonomiaDePrueba.ave();
        taxonomia.tipos.add(anfibio);
        taxonomia.tipos.add(ave);
    }

    private DatosFicha rana(Map<UUID, String> valores) {
        return new DatosFicha(anfibio.getId(), "Animalia", null, "Amphibia", null, null,
                "Oophaga", "histrionica", valores);
    }

    @Test
    void elAtributoNuevoQuedaGuardadoComoDatoYApareceEnElTipo() {
        AtributoDefinido creado = agregar.ejecutar(anfibio.getId(), new NuevoAtributo("Color de piel", TipoDato.TEXTO, false));

        assertThat(creado.id()).isNotNull();
        assertThat(creado.nombre()).isEqualTo("color_de_piel");
        assertThat(taxonomia.definicionesGuardadas).hasSize(1);
        assertThat(new ConsultarTiposEspecie(taxonomia).ejecutar().get(0).atributos())
                .extracting(AtributoDefinido::nombre).contains("color_de_piel");
    }

    // Criterio 1: las fichas nuevas de ese tipo lo incluyen
    @Test
    void lasFichasNuevasDeEseTipoIncluyenElAtributo() {
        AtributoDefinido creado = agregar.ejecutar(anfibio.getId(), new NuevoAtributo("color_de_piel", TipoDato.TEXTO, false));

        FichaTaxonomica ficha = servicioFichas.armar(UUID.randomUUID(), 1, rana(Map.of(
                idDe(anfibio, "temperatura_agua_c"), "24.5",
                creado.id(), "rojo con manchas negras")));

        assertThat(ficha.getValores()).extracting(v -> v.getDefinicion().getNombre())
                .contains("color_de_piel");
    }

    // Si es obligatorio, solo se exige a las fichas nuevas
    @Test
    void unAtributoObligatorioSeExigeEnLasFichasNuevas() {
        agregar.ejecutar(anfibio.getId(), new NuevoAtributo("ph_agua", TipoDato.NUMERO, true));

        FichaInvalidaException error = assertThrows(FichaInvalidaException.class, () -> servicioFichas.armar(
                UUID.randomUUID(), 1, rana(Map.of(idDe(anfibio, "temperatura_agua_c"), "24.5"))));

        assertThat(error.getErrores()).containsExactly("Falta el atributo obligatorio «ph_agua».");
    }

    // Criterio 2: los registros anteriores se conservan y siguen siendo consultables
    @Test
    void lasFichasAnterioresNoSeTocan() {
        FichaTaxonomica anterior = servicioFichas.armar(UUID.randomUUID(), 1,
                rana(Map.of(idDe(anfibio, "temperatura_agua_c"), "24.5")));
        List<String> valoresAntes = anterior.getValores().stream().map(ValorAtributo::getValor).toList();

        agregar.ejecutar(anfibio.getId(), new NuevoAtributo("ph_agua", TipoDato.NUMERO, true));

        assertThat(anterior.getValores()).extracting(ValorAtributo::getValor).isEqualTo(valoresAntes);
        assertThat(anterior.getNumeroVersion()).isEqualTo(1);
    }

    @Test
    void elOtroTipoDeEspecieNoRecibeElAtributo() {
        agregar.ejecutar(anfibio.getId(), new NuevoAtributo("ph_agua", TipoDato.NUMERO, true));

        assertThat(ave.getDefiniciones()).extracting(d -> d.getNombre()).doesNotContain("ph_agua");
    }

    @Test
    void noSePuedeRepetirUnNombreAunqueCambienMayusculasOEspacios() {
        assertThatThrownBy(() -> agregar.ejecutar(anfibio.getId(),
                new NuevoAtributo("  Temperatura Agua C ", TipoDato.NUMERO, false)))
                .isInstanceOf(AtributoInvalidoException.class)
                .hasMessage("El tipo «Anfibio» ya tiene el atributo «temperatura_agua_c».");
        assertThat(taxonomia.definicionesGuardadas).isEmpty();
    }

    @Test
    void sinNombreOSinTipoDeDatoSeRechaza() {
        assertThatThrownBy(() -> agregar.ejecutar(anfibio.getId(), new NuevoAtributo("  ", TipoDato.TEXTO, false)))
                .isInstanceOf(AtributoInvalidoException.class).hasMessage("El nombre del atributo es obligatorio.");
        assertThatThrownBy(() -> agregar.ejecutar(anfibio.getId(), new NuevoAtributo("ph", null, false)))
                .isInstanceOf(AtributoInvalidoException.class).hasMessage("Falta el tipo de dato del atributo.");
    }

    // Valor límite: 100 caracteres sí, 101 no
    @Test
    void elNombrePuedeTenerHasta100Caracteres() {
        assertThat(agregar.ejecutar(anfibio.getId(), new NuevoAtributo("a".repeat(100), TipoDato.TEXTO, false)))
                .isNotNull();
        assertThatThrownBy(() -> agregar.ejecutar(anfibio.getId(), new NuevoAtributo("b".repeat(101), TipoDato.TEXTO, false)))
                .isInstanceOf(AtributoInvalidoException.class);
    }

    @Test
    void unTipoQueNoExisteDa404() {
        assertThatThrownBy(() -> agregar.ejecutar(UUID.randomUUID(), new NuevoAtributo("ph", TipoDato.NUMERO, false)))
                .isInstanceOf(TipoEspecieNoEncontradoException.class);
    }
}
