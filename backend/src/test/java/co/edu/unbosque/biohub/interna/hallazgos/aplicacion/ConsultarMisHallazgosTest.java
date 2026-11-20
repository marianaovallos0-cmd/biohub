package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.AlmacenArchivosFalso;
import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.RepositorioEvidenciasFalso;
import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.RepositorioHallazgosFalso;
import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.RepositorioUbicacionesFalso;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.TipoEvidencia;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.RepositorioFichasFalso;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.RepositorioTaxonomiaFalso;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.DatosFicha;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.ServicioFichas;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.archivo;
import static co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.idDe;
import static org.assertj.core.api.Assertions.assertThat;

// HU-01, criterio 2: las evidencias se pueden consultar después de guardar
class ConsultarMisHallazgosTest {

    private final RepositorioTaxonomiaFalso taxonomia = new RepositorioTaxonomiaFalso();
    private final RepositorioFichasFalso fichas = new RepositorioFichasFalso();
    private final RepositorioHallazgosFalso hallazgos = new RepositorioHallazgosFalso();
    private final RepositorioEvidenciasFalso evidencias = new RepositorioEvidenciasFalso();
    private final ServicioFichas servicioFichas = new ServicioFichas(taxonomia, fichas, Clock.systemUTC());
    private final ConsultarMisHallazgos consultar = new ConsultarMisHallazgos(hallazgos, evidencias, servicioFichas);

    private final UsuarioAutenticado laura = investigador("Laura Gómez");
    private final UsuarioAutenticado andres = investigador("Andrés Rojas");
    private TipoEspecie anfibio;

    @BeforeEach
    void preparar() {
        anfibio = TaxonomiaDePrueba.anfibio();
        taxonomia.tipos.add(anfibio);
    }

    private static UsuarioAutenticado investigador(String nombre) {
        return new UsuarioAutenticado(UUID.randomUUID(), nombre, nombre + "@biohub.test",
                NivelAutorizacion.INVESTIGADOR, Set.of("REGISTRAR_HALLAZGO"));
    }

    // Se registra con el caso de uso real para que la prueba vea lo mismo que vería el investigador
    private void registrar(UsuarioAutenticado autor, Instant cuando, String genero, String especie, int fotos) {
        Clock reloj = Clock.fixed(cuando, ZoneId.of("America/Bogota"));
        RegistrarHallazgo registrar = new RegistrarHallazgo(hallazgos, evidencias, new RepositorioUbicacionesFalso(),
                new ServicioFichas(taxonomia, fichas, reloj), new AlmacenArchivosFalso(), reloj);
        DatosFicha ficha = new DatosFicha(anfibio.getId(), null, null, null, null, null, genero, especie,
                Map.of(idDe(anfibio, "temperatura_agua_c"), "24.5"));
        List<EvidenciaSubida> adjuntas = java.util.stream.IntStream.rangeClosed(1, fotos)
                .mapToObj(i -> new EvidenciaSubida(TipoEvidencia.FOTOGRAFIA, "Foto " + i, archivo("rana" + i + ".jpg", "foto")))
                .toList();
        registrar.ejecutar(new SolicitudRegistro(null, ficha,
                new DatosUbicacion(5.69, -76.66, "Pacífica", "Chocó", false), adjuntas, Map.of()), autor);
    }

    @Test
    void elInvestigadorVeSusHallazgosConSusEvidenciasDelMasReciente() {
        registrar(laura, Instant.parse("2026-11-10T15:00:00Z"), "Oophaga", "histrionica", 2);
        registrar(laura, Instant.parse("2026-11-12T15:00:00Z"), "Atelopus", "varius", 0);

        List<MiHallazgo> mios = consultar.ejecutar(laura);

        assertThat(mios).extracting(MiHallazgo::especie).containsExactly("Atelopus varius", "Oophaga histrionica");
        assertThat(mios.get(1).evidencias()).extracting(EvidenciaRegistrada::descripcion)
                .containsExactly("Foto 1", "Foto 2");
        assertThat(mios.get(1).evidencias()).allSatisfy(e -> assertThat(e.id()).isNotNull());
    }

    @Test
    void noVeLosHallazgosDeOtroInvestigador() {
        registrar(andres, Instant.parse("2026-11-10T15:00:00Z"), "Ara", "macao", 1);

        assertThat(consultar.ejecutar(laura)).isEmpty();
        assertThat(consultar.ejecutar(andres)).hasSize(1);
    }
}
