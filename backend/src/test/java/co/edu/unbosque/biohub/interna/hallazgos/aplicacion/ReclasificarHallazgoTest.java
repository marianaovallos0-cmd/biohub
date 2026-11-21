package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.RepositorioHallazgosFalso;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Hallazgo;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.HallazgoNoEncontradoException;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.ReclasificacionInvalidaException;
import co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.RepositorioCambiosFalso;
import co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.RepositorioFichasConId;
import co.edu.unbosque.biohub.interna.historial.dominio.RegistroCambio;
import co.edu.unbosque.biohub.interna.historial.dominio.VersionDuplicadaException;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.RepositorioTaxonomiaFalso;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.DatosFicha;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.ServicioFichas;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaTaxonomica;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.ValorAtributo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.JUSTIFICACION;
import static co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.REGISTRO;
import static co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.auditor;
import static co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.fichaOriginal;
import static co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.idDe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

// HU-06 y HU-07 con dobles: repositorios en listas, sin base de datos.
class ReclasificarHallazgoTest {

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Bogota"));

    private RepositorioHallazgosFalso hallazgos;
    private RepositorioFichasConId fichas;
    private RepositorioCambiosFalso cambios;
    private ReclasificarHallazgo reclasificar;
    private TipoEspecie anfibio;
    private TipoEspecie ave;
    private Hallazgo hallazgo;
    private FichaTaxonomica original;
    private UsuarioAutenticado auditor;

    @BeforeEach
    void preparar() {
        anfibio = TaxonomiaDePrueba.anfibio();
        ave = TaxonomiaDePrueba.ave();
        RepositorioTaxonomiaFalso taxonomia = new RepositorioTaxonomiaFalso();
        taxonomia.tipos.add(anfibio);
        taxonomia.tipos.add(ave);

        hallazgos = new RepositorioHallazgosFalso();
        hallazgo = new Hallazgo("Expedición Atrato 2026", UUID.randomUUID(), REGISTRO);
        hallazgos.guardar(hallazgo);

        fichas = new RepositorioFichasConId();
        original = fichaOriginal(anfibio, hallazgo.getId());
        fichas.guardar(original);

        cambios = new RepositorioCambiosFalso(fichas.guardadas);
        reclasificar = new ReclasificarHallazgo(hallazgos, new ServicioFichas(taxonomia, fichas, reloj), cambios, reloj);
        auditor = auditor();
    }

    private DatosFicha oophaga() {
        return new DatosFicha(anfibio.getId(), "Animalia", "Chordata", "Amphibia", "Anura", "Dendrobatidae",
                "Oophaga", "histrionica", Map.of(idDe(anfibio, "temperatura_agua_c"), "24.5"));
    }

    @Test
    void creaLaVersionSiguienteYConservaLaAnterior() {
        ReclasificacionRegistrada r = reclasificar.ejecutar(hallazgo.getId(),
                new SolicitudReclasificacion(oophaga(), JUSTIFICACION), auditor);

        assertThat(r.versionAnterior()).isEqualTo(1);
        assertThat(r.versionNueva()).isEqualTo(2);
        assertThat(r.especieAnterior()).isEqualTo("Dendrobates histrionicus");
        assertThat(r.especieNueva()).isEqualTo("Oophaga histrionica");
        assertThat(r.autorizadoPor()).isEqualTo(auditor.nombre());

        assertThat(fichas.guardadas).hasSize(2);
        assertThat(fichas.guardadas.getFirst()).isSameAs(original);
        assertThat(original.getGenero()).isEqualTo("Dendrobates");
        assertThat(fichas.buscarVigente(hallazgo.getId())).get()
                .extracting(FichaTaxonomica::getGenero).isEqualTo("Oophaga");
    }

    @Test
    void guardaElRegistroDelCambioConQuienAutorizo() {
        reclasificar.ejecutar(hallazgo.getId(), new SolicitudReclasificacion(oophaga(), JUSTIFICACION), auditor);

        assertThat(cambios.guardados).hasSize(1);
        RegistroCambio cambio = cambios.guardados.getFirst();
        assertThat(cambio.getAutorizadoPor()).isEqualTo(auditor.id());
        assertThat(cambio.getJustificacion()).isEqualTo(JUSTIFICACION);
        assertThat(cambio.getVersionAnteriorId()).isEqualTo(original.getId());
        assertThat(cambio.getVersionNuevaId()).isEqualTo(fichas.guardadas.get(1).getId());
        assertThat(cambio.getFechaHora().toInstant()).isEqualTo(reloj.instant());
    }

    @Test
    void variasReclasificacionesConservanTodasLasVersiones() {
        reclasificar.ejecutar(hallazgo.getId(), new SolicitudReclasificacion(oophaga(), JUSTIFICACION), auditor);
        DatosFicha otra = new DatosFicha(anfibio.getId(), "Animalia", "Chordata", "Amphibia", "Anura",
                "Dendrobatidae", "Oophaga", "lehmanni", Map.of(idDe(anfibio, "temperatura_agua_c"), "23"));
        reclasificar.ejecutar(hallazgo.getId(), new SolicitudReclasificacion(otra, JUSTIFICACION), auditor);

        assertThat(fichas.listarVersiones(hallazgo.getId()))
                .extracting(FichaTaxonomica::getNumeroVersion).containsExactly(1, 2, 3);
        assertThat(cambios.guardados).hasSize(2);
    }

    @Test
    void sinJustificacionNoGuardaNada() {
        ReclasificacionInvalidaException e = assertThrows(ReclasificacionInvalidaException.class,
                () -> reclasificar.ejecutar(hallazgo.getId(), new SolicitudReclasificacion(oophaga(), "  "), auditor));

        assertThat(e.getErrores()).contains("Falta la justificación del cambio.");
        assertThat(fichas.guardadas).hasSize(1);
        assertThat(cambios.guardados).isEmpty();
    }

    @Test
    void sinAuditorNoGuardaNada() {
        assertThatThrownBy(() -> reclasificar.ejecutar(hallazgo.getId(),
                new SolicitudReclasificacion(oophaga(), JUSTIFICACION), null))
                .isInstanceOf(ReclasificacionInvalidaException.class);
        assertThat(fichas.guardadas).hasSize(1);
    }

    @Test
    void unaFichaInvalidaJuntaTodosLosErrores() {
        DatosFicha sinTemperatura = new DatosFicha(anfibio.getId(), null, null, null, null, null,
                "Oophaga", null, Map.of());

        ReclasificacionInvalidaException e = assertThrows(ReclasificacionInvalidaException.class,
                () -> reclasificar.ejecutar(hallazgo.getId(), new SolicitudReclasificacion(sinTemperatura, ""), auditor));

        assertThat(e.getErrores()).contains("Falta la justificación del cambio.", "Falta la especie.",
                "Falta el atributo obligatorio «temperatura_agua_c».");
        assertThat(fichas.guardadas).hasSize(1);
    }

    @Test
    void rechazaUnaClasificacionIgualALaVigente() {
        DatosFicha igual = new DatosFicha(anfibio.getId(), "Animalia", "Chordata", "Amphibia", "Anura",
                "Dendrobatidae", "Dendrobates", "histrionicus", Map.of(idDe(anfibio, "temperatura_agua_c"), "24.5"));

        ReclasificacionInvalidaException e = assertThrows(ReclasificacionInvalidaException.class,
                () -> reclasificar.ejecutar(hallazgo.getId(), new SolicitudReclasificacion(igual, JUSTIFICACION), auditor));

        assertThat(e.getErrores()).singleElement().asString().contains("igual a la vigente");
    }

    @Test
    void conservaLosArchivosSiElTipoDeEspecieNoCambia() {
        reclasificar.ejecutar(hallazgo.getId(), new SolicitudReclasificacion(oophaga(), JUSTIFICACION), auditor);

        FichaTaxonomica nueva = fichas.guardadas.get(1);
        assertThat(nueva.getValores()).extracting(ValorAtributo::getValor)
                .containsExactlyInAnyOrder("24.5", "evidencias/canto-001.wav");
    }

    @Test
    void puedeCambiarElTipoDeEspecie() {
        DatosFicha comoAve = new DatosFicha(ave.getId(), "Animalia", "Chordata", "Aves", "Psittaciformes",
                "Psittacidae", "Ara", "macao", Map.of(idDe(ave, "ruta_gps"), "rutas/atrato-001.gpx"));

        ReclasificacionRegistrada r = reclasificar.ejecutar(hallazgo.getId(),
                new SolicitudReclasificacion(comoAve, JUSTIFICACION), auditor);

        assertThat(r.especieNueva()).isEqualTo("Ara macao");
        assertThat(fichas.guardadas.get(1).getTipoEspecie()).isSameAs(ave);
    }

    @Test
    void unHallazgoQueNoExisteDa404() {
        assertThatThrownBy(() -> reclasificar.ejecutar(UUID.randomUUID(),
                new SolicitudReclasificacion(oophaga(), JUSTIFICACION), auditor))
                .isInstanceOf(HallazgoNoEncontradoException.class);
    }

    @Test
    void siOtroAuditorGuardoLaMismaVersionSeAvisa() {
        cambios.chocarAlGuardar = true;

        assertThatThrownBy(() -> reclasificar.ejecutar(hallazgo.getId(),
                new SolicitudReclasificacion(oophaga(), JUSTIFICACION), auditor))
                .isInstanceOf(VersionDuplicadaException.class);
    }
}
