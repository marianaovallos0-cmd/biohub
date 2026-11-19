package co.edu.unbosque.biohub.interna.taxonomia.aplicacion;

import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.RepositorioFichasFalso;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.RepositorioTaxonomiaFalso;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaInvalidaException;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaTaxonomica;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.idDe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServicioFichasTest {

    private static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneId.of("America/Bogota"));

    private final RepositorioTaxonomiaFalso taxonomia = new RepositorioTaxonomiaFalso();
    private final RepositorioFichasFalso fichas = new RepositorioFichasFalso();
    private final ServicioFichas servicio = new ServicioFichas(taxonomia, fichas, RELOJ);
    private TipoEspecie anfibio;

    @BeforeEach
    void preparar() {
        anfibio = TaxonomiaDePrueba.anfibio();
        taxonomia.tipos.add(anfibio);
        taxonomia.tipos.add(TaxonomiaDePrueba.ave());
    }

    private DatosFicha rana(Map<UUID, String> valores) {
        return new DatosFicha(anfibio.getId(), "Animalia", "Chordata", "Amphibia", "Anura",
                "Dendrobatidae", "Oophaga", "histrionica", valores);
    }

    @Test
    void armaLaVersionConLaHoraDeColombiaSinGuardarla() {
        FichaTaxonomica ficha = servicio.armar(UUID.randomUUID(), 1,
                rana(Map.of(idDe(anfibio, "temperatura_agua_c"), "24.5")));

        assertThat(ficha.getFechaVersion()).isEqualTo(OffsetDateTime.parse("2026-11-13T10:00-05:00"));
        assertThat(fichas.guardadas).isEmpty();
    }

    @Test
    void guardaSoloCuandoSeLePide() {
        FichaTaxonomica ficha = servicio.armar(UUID.randomUUID(), 1,
                rana(Map.of(idDe(anfibio, "temperatura_agua_c"), "24.5")));

        servicio.guardar(ficha);

        assertThat(fichas.guardadas).containsExactly(ficha);
    }

    @Test
    void unAtributoArchivoSoloTomaLaRutaDeUnArchivoSubido() {
        UUID temperatura = idDe(anfibio, "temperatura_agua_c");
        UUID grabacion = idDe(anfibio, "grabacion_acustica");

        FichaTaxonomica escritaAMano = servicio.armar(UUID.randomUUID(), 1,
                rana(Map.of(temperatura, "24.5", grabacion, "evidencias/de-otro-hallazgo.mp3")));
        FichaTaxonomica conArchivo = servicio.armar(UUID.randomUUID(), 1,
                rana(Map.of(temperatura, "24.5")), Map.of(grabacion, "evidencias/canto.mp3"));

        assertThat(escritaAMano.getValores()).extracting(v -> v.getDefinicion().getNombre())
                .containsExactly("temperatura_agua_c");
        assertThat(conArchivo.getValores()).extracting(v -> v.getValor())
                .containsExactlyInAnyOrder("24.5", "evidencias/canto.mp3");
    }

    @Test
    void unTipoDeEspecieQueNoExisteSeRechaza() {
        DatosFicha datos = new DatosFicha(UUID.randomUUID(), null, null, null, null, null, "Ara", "macao", Map.of());

        FichaInvalidaException error = assertThrows(FichaInvalidaException.class,
                () -> servicio.armar(UUID.randomUUID(), 1, datos));

        assertThat(error.getErrores()).containsExactly("El tipo de especie no existe.");
    }

    @Test
    void sinTipoDeEspecieSeRechaza() {
        DatosFicha datos = new DatosFicha(null, null, null, null, null, null, "Ara", "macao", Map.of());

        assertThatThrownBy(() -> servicio.armar(UUID.randomUUID(), 1, datos))
                .isInstanceOf(FichaInvalidaException.class);
    }

    @Test
    void listaCadaTipoConSusPropiosAtributos() {
        var tipos = new ConsultarTiposEspecie(taxonomia).ejecutar();

        assertThat(tipos).extracting(TipoEspecieConAtributos::nombre).containsExactly("Anfibio", "Ave");
        assertThat(tipos.get(0).atributos()).extracting(AtributoDefinido::nombre)
                .containsExactly("temperatura_agua_c", "grabacion_acustica", "longitud_hocico_cloaca_mm");
        assertThat(tipos.get(1).atributos()).extracting(AtributoDefinido::nombre)
                .containsExactly("ruta_gps", "muestra_adn", "fecha_anillado");
    }
}
