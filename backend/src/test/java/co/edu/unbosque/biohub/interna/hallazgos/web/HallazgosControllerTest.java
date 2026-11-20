package co.edu.unbosque.biohub.interna.hallazgos.web;

import co.edu.unbosque.biohub.interna.hallazgos.dominio.AlmacenArchivos;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Evidencia;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Hallazgo;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioEvidencias;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioHallazgos;
import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba;
import co.edu.unbosque.biohub.interna.seguridad.dominio.IntentoAccesoNoAutorizado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioIntentos;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioUsuarios;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaTaxonomica;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.RepositorioFichas;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.RepositorioTaxonomia;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.RepositorioUbicaciones;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.Ubicacion;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.CLAVE;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.rol;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.usuario;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.ADMINISTRADOR;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.INVESTIGADOR;
import static co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.idDe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// HU-01 de punta a punta con el perfil interna. Los repositorios y el disco son dobles, no hace falta PostgreSQL.
@SpringBootTest(properties = {
        "biohub.jwt.secreto=clave-solo-para-las-pruebas-de-biohub-2026",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("interna")
class HallazgosControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RepositorioUsuarios repositorioUsuarios;

    @MockitoBean
    RepositorioIntentos repositorioIntentos;

    @MockitoBean
    RepositorioTaxonomia repositorioTaxonomia;

    @MockitoBean
    RepositorioFichas repositorioFichas;

    @MockitoBean
    RepositorioHallazgos repositorioHallazgos;

    @MockitoBean
    RepositorioEvidencias repositorioEvidencias;

    @MockitoBean
    RepositorioUbicaciones repositorioUbicaciones;

    @MockitoBean
    AlmacenArchivos almacen;

    @MockitoBean
    PlatformTransactionManager transacciones;

    private Usuario laura;
    private Usuario andres;
    private TipoEspecie anfibio;

    @BeforeEach
    void preparar() {
        laura = usuario("Laura Gómez", "investigador1@biohub.test", rol(INVESTIGADOR, "REGISTRAR_HALLAZGO"));
        andres = usuario("Andrés Rojas", "investigador2@biohub.test", rol(INVESTIGADOR, "REGISTRAR_HALLAZGO"));
        Usuario admin = usuario("Admin BioHub", "admin@biohub.test", rol(ADMINISTRADOR, "GESTIONAR_EQUIPOS"));
        List<Usuario> usuarios = List.of(laura, andres, admin);

        when(repositorioUsuarios.buscarPorCorreo(anyString())).thenAnswer(i -> usuarios.stream()
                .filter(u -> u.getCorreo().equals(i.getArgument(0))).findFirst());
        when(repositorioUsuarios.buscarPorId(any(UUID.class))).thenAnswer(i -> usuarios.stream()
                .filter(u -> u.getId().equals(i.getArgument(0))).findFirst());

        anfibio = TaxonomiaDePrueba.anfibio();
        when(repositorioTaxonomia.buscarTipo(anfibio.getId())).thenReturn(Optional.of(anfibio));
        when(almacen.nuevaRuta(anyString())).thenAnswer(i -> "evidencias/" + UUID.randomUUID() + "." + i.getArgument(0));
    }

    private String iniciarSesion(String correo) throws Exception {
        String json = mvc.perform(post("/interno/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private MockMultipartFile datos(String temperatura) {
        String json = """
                {
                  "expedicion": "Expedición Atrato 2026",
                  "ficha": {
                    "tipoEspecieId": "%s",
                    "reino": "Animalia", "clase": "Amphibia",
                    "genero": "Oophaga", "especie": "histrionica",
                    "valores": { "%s": "%s" }
                  },
                  "ubicacion": {
                    "latitud": 5.6947, "longitud": -76.6611,
                    "region": "Pacífica", "departamento": "Chocó", "especieProtegida": true
                  },
                  "evidencias": [ { "tipo": "FOTOGRAFIA", "descripcion": "Ejemplar en hojarasca" } ]
                }
                """.formatted(anfibio.getId(), idDe(anfibio, "temperatura_agua_c"), temperatura);
        return new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes());
    }

    private static MockMultipartFile foto() {
        return new MockMultipartFile("evidencias", "rana.jpg", MediaType.IMAGE_JPEG_VALUE, "foto".getBytes());
    }

    @Test
    void elInvestigadorRegistraUnHallazgoConSuFotoYQuedaComoAutor() throws Exception {
        String token = iniciarSesion("investigador1@biohub.test");

        long inicio = System.nanoTime();
        mvc.perform(multipart("/interno/hallazgos").file(datos("24.5")).file(foto())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.autor").value("Laura Gómez"))
                .andExpect(jsonPath("$.especie").value("Oophaga histrionica"))
                .andExpect(jsonPath("$.evidencias.length()").value(1))
                .andExpect(jsonPath("$.evidencias[0].tipo").value("FOTOGRAFIA"))
                .andExpect(jsonPath("$.evidencias[0].nombreOriginal").value("rana.jpg"));
        // Criterio 4: la petición completa (seguridad, validación y guardado) en menos de 5 s
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(5));

        ArgumentCaptor<Hallazgo> hallazgo = ArgumentCaptor.forClass(Hallazgo.class);
        verify(repositorioHallazgos).guardar(hallazgo.capture());
        assertThat(hallazgo.getValue().getInvestigadorId()).isEqualTo(laura.getId());

        ArgumentCaptor<FichaTaxonomica> ficha = ArgumentCaptor.forClass(FichaTaxonomica.class);
        verify(repositorioFichas).guardar(ficha.capture());
        assertThat(ficha.getValue().getHallazgoId()).isEqualTo(hallazgo.getValue().getId());

        ArgumentCaptor<Ubicacion> ubicacion = ArgumentCaptor.forClass(Ubicacion.class);
        verify(repositorioUbicaciones).guardar(ubicacion.capture());
        assertThat(ubicacion.getValue().getDepartamento()).isEqualTo("Chocó");

        ArgumentCaptor<Evidencia> evidencia = ArgumentCaptor.forClass(Evidencia.class);
        verify(repositorioEvidencias).guardar(evidencia.capture());
        assertThat(evidencia.getValue().getDescripcion()).isEqualTo("Ejemplar en hojarasca");
        verify(almacen).guardar(any(), any());
    }

    @Test
    void siFaltaUnDatoObligatorioResponde400ConLoQueFaltaYNoGuardaNada() throws Exception {
        String token = iniciarSesion("investigador1@biohub.test");

        mvc.perform(multipart("/interno/hallazgos").file(datos("")).file(foto())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("Falta el atributo obligatorio «temperatura_agua_c»."));

        verify(repositorioHallazgos, never()).guardar(any());
        verify(repositorioFichas, never()).guardar(any());
        verify(almacen, never()).guardar(any(), any());
    }

    @Test
    void unRolSinPermisoNoRegistraYSuIntentoQuedaGuardado() throws Exception {
        String token = iniciarSesion("admin@biohub.test");

        mvc.perform(multipart("/interno/hallazgos").file(datos("24.5")).file(foto())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verify(repositorioIntentos).guardar(any(IntentoAccesoNoAutorizado.class));
        verify(repositorioHallazgos, never()).guardar(any());
    }

    @Test
    void elAutorDescargaSuEvidencia() throws Exception {
        Evidencia foto = evidenciaDe(laura);
        when(almacen.leer(foto.getUrlArchivo())).thenReturn(Optional.of(new ByteArrayInputStream("foto".getBytes())));
        String token = iniciarSesion("investigador1@biohub.test");

        mvc.perform(get("/interno/evidencias/" + foto.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().string("foto"));
    }

    @Test
    void otroInvestigadorNoDescargaLaEvidenciaYSuIntentoQuedaGuardado() throws Exception {
        Evidencia foto = evidenciaDe(laura);
        String token = iniciarSesion("investigador2@biohub.test");

        mvc.perform(get("/interno/evidencias/" + foto.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        ArgumentCaptor<IntentoAccesoNoAutorizado> intento = ArgumentCaptor.forClass(IntentoAccesoNoAutorizado.class);
        verify(repositorioIntentos).guardar(intento.capture());
        assertThat(intento.getValue().getUsuario()).isSameAs(andres);
        assertThat(intento.getValue().getRecursoSolicitado()).isEqualTo("GET /interno/evidencias/" + foto.getId());
        verify(almacen, never()).leer(anyString());
    }

    // Criterio 2: el investigador vuelve a encontrar sus evidencias después de guardar
    @Test
    void elInvestigadorConsultaSusHallazgosConSusEvidencias() throws Exception {
        Hallazgo hallazgo = new Hallazgo("Expedición Atrato 2026", laura.getId(), OffsetDateTime.parse("2026-11-13T10:00-05:00"));
        Evidencia foto = HallazgosDePrueba.evidencia(hallazgo.getId(), "evidencias/foto-1.jpg");
        when(repositorioHallazgos.listarPorInvestigador(laura.getId())).thenReturn(List.of(hallazgo));
        when(repositorioEvidencias.listarPorHallazgo(hallazgo.getId())).thenReturn(List.of(foto));
        String token = iniciarSesion("investigador1@biohub.test");

        mvc.perform(get("/interno/hallazgos/mios").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].expedicion").value("Expedición Atrato 2026"))
                .andExpect(jsonPath("$[0].evidencias[0].id").value(foto.getId().toString()));
    }

    @Test
    void unRolSinPermisoNoConsultaHallazgosYSuIntentoQuedaGuardado() throws Exception {
        String token = iniciarSesion("admin@biohub.test");

        mvc.perform(get("/interno/hallazgos/mios").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verify(repositorioIntentos).guardar(any(IntentoAccesoNoAutorizado.class));
    }

    private Evidencia evidenciaDe(Usuario autor) {
        Hallazgo hallazgo = new Hallazgo(null, autor.getId(), OffsetDateTime.parse("2026-11-13T10:00-05:00"));
        Evidencia evidencia = HallazgosDePrueba.evidencia(hallazgo.getId(), "evidencias/foto-1.jpg");
        when(repositorioHallazgos.buscarPorId(hallazgo.getId())).thenReturn(Optional.of(hallazgo));
        when(repositorioEvidencias.buscarPorId(evidencia.getId())).thenReturn(Optional.of(evidencia));
        return evidencia;
    }
}
