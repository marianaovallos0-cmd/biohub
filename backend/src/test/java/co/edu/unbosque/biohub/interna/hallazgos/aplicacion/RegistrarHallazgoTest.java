package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.AlmacenArchivosFalso;
import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.RepositorioEvidenciasFalso;
import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.RepositorioHallazgosFalso;
import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.RepositorioUbicacionesFalso;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Evidencia;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Hallazgo;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.HallazgoInvalidoException;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.ReglasArchivos;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.TipoEvidencia;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.RepositorioFichasFalso;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.RepositorioTaxonomiaFalso;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.DatosFicha;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.ServicioFichas;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaTaxonomica;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.Ubicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.archivo;
import static co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.archivoQueNoSeDebeAbrir;
import static co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.idDe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class RegistrarHallazgoTest {

    private static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-13T10:00-05:00");

    private final RepositorioTaxonomiaFalso taxonomia = new RepositorioTaxonomiaFalso();
    private final RepositorioFichasFalso fichas = new RepositorioFichasFalso();
    private final RepositorioHallazgosFalso hallazgos = new RepositorioHallazgosFalso();
    private final RepositorioEvidenciasFalso evidencias = new RepositorioEvidenciasFalso();
    private final RepositorioUbicacionesFalso ubicaciones = new RepositorioUbicacionesFalso();
    private final AlmacenArchivosFalso almacen = new AlmacenArchivosFalso();
    private final RegistrarHallazgo registrar = new RegistrarHallazgo(hallazgos, evidencias, ubicaciones,
            new ServicioFichas(taxonomia, fichas, RELOJ), almacen, RELOJ);

    private final UsuarioAutenticado laura = new UsuarioAutenticado(UUID.randomUUID(), "Laura Gómez",
            "investigador1@biohub.test", NivelAutorizacion.INVESTIGADOR, Set.of("REGISTRAR_HALLAZGO"));
    private TipoEspecie anfibio;

    @BeforeEach
    void preparar() {
        anfibio = TaxonomiaDePrueba.anfibio();
        taxonomia.tipos.add(anfibio);
    }

    private DatosFicha rana(Map<UUID, String> valores) {
        return new DatosFicha(anfibio.getId(), "Animalia", "Chordata", "Amphibia", "Anura",
                "Dendrobatidae", "Oophaga", "histrionica", valores);
    }

    private DatosFicha ranaCompleta() {
        return rana(Map.of(idDe(anfibio, "temperatura_agua_c"), "24.5"));
    }

    private static DatosUbicacion choco() {
        return new DatosUbicacion(5.6947, -76.6611, "Pacífica", "Chocó", true);
    }

    private void sinNadaGuardado() {
        assertThat(hallazgos.guardados).isEmpty();
        assertThat(fichas.guardadas).isEmpty();
        assertThat(ubicaciones.guardadas).isEmpty();
        assertThat(evidencias.guardadas).isEmpty();
        assertThat(almacen.archivos).isEmpty();
    }

    // Criterio 1: queda almacenado y asociado a su autor, con fecha y hora
    @Test
    void registraElHallazgoConSuAutorYLaFechaYHora() {
        HallazgoRegistrado registrado = registrar.ejecutar(
                new SolicitudRegistro("Expedición Atrato 2026", ranaCompleta(), choco(), List.of(), Map.of()), laura);

        Hallazgo hallazgo = hallazgos.guardados.get(0);
        assertThat(hallazgo.getInvestigadorId()).isEqualTo(laura.id());
        assertThat(hallazgo.getFechaRegistro()).isEqualTo(AHORA);
        assertThat(hallazgo.getExpedicion()).isEqualTo("Expedición Atrato 2026");
        assertThat(registrado.id()).isEqualTo(hallazgo.getId());
        assertThat(registrado.especie()).isEqualTo("Oophaga histrionica");

        FichaTaxonomica ficha = fichas.guardadas.get(0);
        assertThat(ficha.getHallazgoId()).isEqualTo(hallazgo.getId());
        assertThat(ficha.getNumeroVersion()).isEqualTo(1);

        Ubicacion ubicacion = ubicaciones.guardadas.get(0);
        assertThat(ubicacion.getHallazgoId()).isEqualTo(hallazgo.getId());
        assertThat(ubicacion.isEspecieProtegida()).isTrue();
    }

    // Criterio 2: las evidencias quedan asociadas y se pueden consultar después
    @Test
    void lasEvidenciasQuedanAsociadasAlHallazgo() {
        List<EvidenciaSubida> adjuntas = List.of(
                new EvidenciaSubida(TipoEvidencia.FOTOGRAFIA, "Ejemplar en hojarasca", archivo("rana.JPG", "foto")),
                new EvidenciaSubida(TipoEvidencia.AUDIO, null, archivo("canto.wav", "audio")));

        HallazgoRegistrado registrado = registrar.ejecutar(
                new SolicitudRegistro(null, ranaCompleta(), choco(), adjuntas, Map.of()), laura);

        // Criterio 2: cada evidencia vuelve con su id para consultarla después
        assertThat(registrado.evidencias()).extracting(EvidenciaRegistrada::id)
                .containsExactlyElementsOf(evidencias.guardadas.stream().map(Evidencia::getId).toList())
                .doesNotContainNull();
        assertThat(registrado.evidencias()).extracting(EvidenciaRegistrada::nombreOriginal)
                .containsExactly("rana.JPG", "canto.wav");
        assertThat(evidencias.guardadas).extracting(Evidencia::getHallazgoId).containsOnly(registrado.id());
        assertThat(evidencias.guardadas).extracting(Evidencia::getUrlArchivo)
                .containsExactly("evidencias/archivo-1.jpg", "evidencias/archivo-2.wav");
        assertThat(almacen.archivos).containsEntry("evidencias/archivo-1.jpg", "foto")
                .containsEntry("evidencias/archivo-2.wav", "audio");
        assertThat(evidencias.guardadas.get(0).getFechaCarga()).isEqualTo(AHORA);
    }

    // Criterio 4: el registro termina en menos de 5 segundos (aquí sin base de datos; con Docker se ve en el log)
    @Test
    void registraEnMenosDe5Segundos() {
        List<EvidenciaSubida> adjuntas = List.of(
                new EvidenciaSubida(TipoEvidencia.FOTOGRAFIA, null, archivo("rana.jpg", "foto")));

        assertTimeout(Duration.ofSeconds(5), () -> registrar.ejecutar(
                new SolicitudRegistro(null, ranaCompleta(), choco(), adjuntas, Map.of()), laura));
    }

    @Test
    void unAtributoDeTipoArchivoGuardaLaRutaComoValor() {
        UUID grabacion = idDe(anfibio, "grabacion_acustica");

        registrar.ejecutar(new SolicitudRegistro(null, ranaCompleta(), choco(), List.of(),
                Map.of(grabacion, archivo("canto.mp3", "audio"))), laura);

        FichaTaxonomica ficha = fichas.guardadas.get(0);
        assertThat(ficha.getValores()).anySatisfy(v -> {
            assertThat(v.getDefinicion().getId()).isEqualTo(grabacion);
            assertThat(v.getValor()).isEqualTo("evidencias/archivo-1.mp3");
        });
        assertThat(almacen.archivos).containsOnlyKeys("evidencias/archivo-1.mp3");
    }

    // Criterio 3: si falta un dato obligatorio no se guarda y se indica qué falta
    @Test
    void siFaltanDatosNoGuardaNadaYDiceTodoLoQueFalta() {
        DatosUbicacion sinRegion = new DatosUbicacion(5.69, -76.66, " ", "Chocó", false);
        List<EvidenciaSubida> adjuntas = List.of(
                new EvidenciaSubida(TipoEvidencia.FOTOGRAFIA, null, archivo("rana.jpg", "foto")));

        HallazgoInvalidoException error = assertThrows(HallazgoInvalidoException.class, () -> registrar.ejecutar(
                new SolicitudRegistro(null, rana(Map.of()), sinRegion, adjuntas, Map.of()), laura));

        assertThat(error.getErrores()).containsExactly(
                "Falta la región.",
                "Falta el atributo obligatorio «temperatura_agua_c».");
        sinNadaGuardado();
    }

    @Test
    void sinUbicacionNoSeGuarda() {
        HallazgoInvalidoException error = assertThrows(HallazgoInvalidoException.class, () -> registrar.ejecutar(
                new SolicitudRegistro(null, ranaCompleta(), null, List.of(), Map.of()), laura));

        assertThat(error.getErrores()).containsExactly("Falta la ubicación del hallazgo.");
        sinNadaGuardado();
    }

    @Test
    void rechazaUnFormatoQueNoCorrespondeAlTipoDeEvidencia() {
        List<EvidenciaSubida> adjuntas = List.of(
                new EvidenciaSubida(TipoEvidencia.FOTOGRAFIA, null, archivo("virus.exe", "x")),
                new EvidenciaSubida(TipoEvidencia.AUDIO, null, archivo("canto.jpg", "x")));

        HallazgoInvalidoException error = assertThrows(HallazgoInvalidoException.class, () -> registrar.ejecutar(
                new SolicitudRegistro(null, ranaCompleta(), choco(), adjuntas, Map.of()), laura));

        assertThat(error.getErrores()).containsExactly(
                "Evidencia 1 (virus.exe): para FOTOGRAFIA solo se acepta jpeg, jpg, png.",
                "Evidencia 2 (canto.jpg): para AUDIO solo se acepta mp3, wav.");
        sinNadaGuardado();
    }

    // Valores límite del tamaño: 10 MB exactos sí, un byte más no, y vacío no
    @Test
    void elLimiteEsDe10MBYNoSeAceptanArchivosVacios() {
        long limite = ReglasArchivos.MAXIMO_BYTES;
        List<EvidenciaSubida> adjuntas = List.of(
                new EvidenciaSubida(TipoEvidencia.MUESTRA, null, archivoQueNoSeDebeAbrir("lab.pdf", limite + 1)),
                new EvidenciaSubida(TipoEvidencia.MUESTRA, null, archivoQueNoSeDebeAbrir("vacio.pdf", 0)));

        HallazgoInvalidoException error = assertThrows(HallazgoInvalidoException.class, () -> registrar.ejecutar(
                new SolicitudRegistro(null, ranaCompleta(), choco(), adjuntas, Map.of()), laura));

        assertThat(error.getErrores()).containsExactly(
                "Evidencia 1 (lab.pdf): pesa más de 10 MB.",
                "Evidencia 2 (vacio.pdf): el archivo está vacío.");
        assertThat(ReglasArchivos.erroresDeTamano("x", limite)).isEmpty();
    }

    @Test
    void seAceptanMaximo5Evidencias() {
        List<EvidenciaSubida> seis = java.util.stream.IntStream.rangeClosed(1, 6)
                .mapToObj(i -> new EvidenciaSubida(TipoEvidencia.FOTOGRAFIA, null, archivo("rana" + i + ".jpg", "foto")))
                .toList();

        HallazgoInvalidoException error = assertThrows(HallazgoInvalidoException.class, () -> registrar.ejecutar(
                new SolicitudRegistro(null, ranaCompleta(), choco(), seis, Map.of()), laura));

        assertThat(error.getErrores()).containsExactly("Se pueden adjuntar máximo 5 evidencias por hallazgo.");
        sinNadaGuardado();
    }

    @Test
    void unDepartamentoMuyLargoDiceQueEsMuyLargoYNoQueFalta() {
        DatosUbicacion larga = new DatosUbicacion(5.69, -76.66, "Pacífica", "x".repeat(101), false);

        HallazgoInvalidoException error = assertThrows(HallazgoInvalidoException.class, () -> registrar.ejecutar(
                new SolicitudRegistro(null, ranaCompleta(), larga, List.of(), Map.of()), laura));

        assertThat(error.getErrores()).containsExactly("El departamento tiene más de 100 caracteres.");
    }

    @Test
    void unaEvidenciaSinArchivoSeRechaza() {
        List<EvidenciaSubida> adjuntas = List.of(new EvidenciaSubida(TipoEvidencia.FOTOGRAFIA, null, null));

        HallazgoInvalidoException error = assertThrows(HallazgoInvalidoException.class, () -> registrar.ejecutar(
                new SolicitudRegistro(null, ranaCompleta(), choco(), adjuntas, Map.of()), laura));

        assertThat(error.getErrores()).containsExactly("Evidencia 1: falta el archivo.");
    }

    @Test
    void unArchivoEnUnAtributoQueNoEsDeTipoArchivoSeRechaza() {
        UUID temperatura = idDe(anfibio, "temperatura_agua_c");

        HallazgoInvalidoException error = assertThrows(HallazgoInvalidoException.class, () -> registrar.ejecutar(
                new SolicitudRegistro(null, rana(Map.of()), choco(), List.of(),
                        Map.of(temperatura, archivo("temp.png", "x"))), laura));

        assertThat(error.getErrores()).containsExactly(
                "El atributo «temperatura_agua_c» debe ser un número (use punto para los decimales).");
        sinNadaGuardado();
    }
}
