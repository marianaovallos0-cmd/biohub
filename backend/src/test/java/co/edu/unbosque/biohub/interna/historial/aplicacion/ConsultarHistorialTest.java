package co.edu.unbosque.biohub.interna.historial.aplicacion;

import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.RepositorioHallazgosFalso;
import co.edu.unbosque.biohub.interna.hallazgos.aplicacion.ReclasificarHallazgo;
import co.edu.unbosque.biohub.interna.hallazgos.aplicacion.SolicitudReclasificacion;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Hallazgo;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.HallazgoNoEncontradoException;
import co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.RepositorioCambiosFalso;
import co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.RepositorioFichasConId;
import co.edu.unbosque.biohub.interna.seguridad.RepositorioUsuariosFalso;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.RepositorioTaxonomiaFalso;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.DatosFicha;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.ServicioFichas;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.JUSTIFICACION;
import static co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.REGISTRO;
import static co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.fichaOriginal;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.rol;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.usuario;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.AUDITOR;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.INVESTIGADOR;
import static co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.idDe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// HU-08 (y HU-07: cada versión muestra quién la autorizó)
class ConsultarHistorialTest {

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

    private ConsultarHistorial consultar;
    private ReclasificarHallazgo reclasificar;
    private Hallazgo hallazgo;
    private TipoEspecie anfibio;
    private Usuario investigador;
    private UsuarioAutenticado auditor;

    @BeforeEach
    void preparar() {
        anfibio = TaxonomiaDePrueba.anfibio();
        RepositorioTaxonomiaFalso taxonomia = new RepositorioTaxonomiaFalso();
        taxonomia.tipos.add(anfibio);
        RepositorioFichasConId fichas = new RepositorioFichasConId();
        RepositorioCambiosFalso cambios = new RepositorioCambiosFalso(fichas.guardadas);
        ServicioFichas servicioFichas = new ServicioFichas(taxonomia, fichas, reloj);

        investigador = usuario("Laura Gómez", "investigador1@biohub.test", rol(INVESTIGADOR, "REGISTRAR_HALLAZGO"));
        Usuario carolina = usuario("Carolina Vélez", "auditor@biohub.test", rol(AUDITOR, "RECLASIFICAR_HALLAZGO"));
        RepositorioUsuariosFalso usuarios = new RepositorioUsuariosFalso();
        usuarios.guardar(investigador);
        usuarios.guardar(carolina);
        auditor = UsuarioAutenticado.de(carolina);

        RepositorioHallazgosFalso hallazgos = new RepositorioHallazgosFalso();
        hallazgo = new Hallazgo("Expedición Atrato 2026", investigador.getId(), REGISTRO);
        hallazgos.guardar(hallazgo);
        fichas.guardar(fichaOriginal(anfibio, hallazgo.getId()));

        reclasificar = new ReclasificarHallazgo(hallazgos, servicioFichas, cambios, reloj);
        consultar = new ConsultarHistorial(hallazgos, servicioFichas, cambios, usuarios);
    }

    private void reclasificarComo(String especie, String temperatura) {
        DatosFicha datos = new DatosFicha(anfibio.getId(), "Animalia", "Chordata", "Amphibia", "Anura",
                "Dendrobatidae", "Oophaga", especie, Map.of(idDe(anfibio, "temperatura_agua_c"), temperatura));
        reclasificar.ejecutar(hallazgo.getId(), new SolicitudReclasificacion(datos, JUSTIFICACION), auditor);
    }

    @Test
    void muestraTodasLasVersionesEnOrdenCronologico() {
        reclasificarComo("histrionica", "24.5");
        reclasificarComo("lehmanni", "22");

        HistorialHallazgo historial = consultar.ejecutar(hallazgo.getId());

        assertThat(historial.versionVigente()).isEqualTo(3);
        assertThat(historial.investigador()).isEqualTo("Laura Gómez");
        assertThat(historial.versiones()).extracting(HistorialHallazgo.Version::numeroVersion).containsExactly(1, 2, 3);
        assertThat(historial.versiones()).extracting(HistorialHallazgo.Version::vigente).containsExactly(false, false, true);
        assertThat(historial.versiones()).extracting(v -> v.genero() + " " + v.especie())
                .containsExactly("Dendrobates histrionicus", "Oophaga histrionica", "Oophaga lehmanni");
    }

    @Test
    void cadaVersionMuestraSusPropiosAtributos() {
        reclasificarComo("histrionica", "21");

        HistorialHallazgo historial = consultar.ejecutar(hallazgo.getId());

        assertThat(historial.versiones().get(0).atributos())
                .extracting(HistorialHallazgo.Atributo::nombre, HistorialHallazgo.Atributo::valor)
                .contains(org.assertj.core.groups.Tuple.tuple("temperatura_agua_c", "24.5"));
        assertThat(historial.versiones().get(1).atributos())
                .extracting(HistorialHallazgo.Atributo::nombre, HistorialHallazgo.Atributo::valor)
                .contains(org.assertj.core.groups.Tuple.tuple("temperatura_agua_c", "21"));
    }

    @Test
    void laVersionOriginalNoTieneCambioYLasDemasDicenQuienLasAutorizo() {
        reclasificarComo("histrionica", "24.5");

        HistorialHallazgo historial = consultar.ejecutar(hallazgo.getId());

        assertThat(historial.versiones().get(0).cambio()).isNull();
        HistorialHallazgo.Cambio cambio = historial.versiones().get(1).cambio();
        assertThat(cambio.autorizadoPor()).isEqualTo("Carolina Vélez");
        assertThat(cambio.justificacion()).isEqualTo(JUSTIFICACION);
        assertThat(cambio.versionAnterior()).isEqualTo(1);
        assertThat(cambio.fechaHora()).isNotNull();
    }

    @Test
    void unHallazgoSinReclasificarTieneUnaSolaVersion() {
        HistorialHallazgo historial = consultar.ejecutar(hallazgo.getId());

        assertThat(historial.versiones()).singleElement()
                .satisfies(v -> assertThat(v.vigente()).isTrue());
    }

    @Test
    void unHallazgoQueNoExisteDaError() {
        assertThatThrownBy(() -> consultar.ejecutar(UUID.randomUUID()))
                .isInstanceOf(HallazgoNoEncontradoException.class);
    }
}
