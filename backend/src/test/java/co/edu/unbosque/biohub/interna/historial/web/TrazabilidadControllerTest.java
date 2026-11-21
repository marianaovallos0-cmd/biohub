package co.edu.unbosque.biohub.interna.historial.web;

import co.edu.unbosque.biohub.interna.hallazgos.dominio.AlmacenArchivos;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Hallazgo;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioEvidencias;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioHallazgos;
import co.edu.unbosque.biohub.interna.historial.dominio.ConsultaHallazgos;
import co.edu.unbosque.biohub.interna.historial.dominio.RegistroCambio;
import co.edu.unbosque.biohub.interna.historial.dominio.RepositorioCambios;
import co.edu.unbosque.biohub.interna.historial.dominio.ResumenHallazgo;
import co.edu.unbosque.biohub.interna.historial.dominio.VersionDuplicadaException;
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
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.JUSTIFICACION;
import static co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.REGISTRO;
import static co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.conId;
import static co.edu.unbosque.biohub.interna.historial.TrazabilidadDePrueba.fichaOriginal;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.CLAVE;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.rol;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.usuario;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.AUDITOR;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.INVESTIGADOR;
import static co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.idDe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// HU-06, HU-07 y HU-08 de punta a punta con el perfil interna: login, permisos, controladores,
// casos de uso y transacción. Los repositorios son dobles, no hace falta PostgreSQL.
@SpringBootTest(properties = {
        "biohub.jwt.secreto=clave-solo-para-las-pruebas-de-biohub-2026",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("interna")
class TrazabilidadControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean RepositorioUsuarios repositorioUsuarios;
    @MockitoBean RepositorioIntentos repositorioIntentos;
    @MockitoBean RepositorioTaxonomia repositorioTaxonomia;
    @MockitoBean RepositorioFichas repositorioFichas;
    @MockitoBean RepositorioHallazgos repositorioHallazgos;
    @MockitoBean RepositorioEvidencias repositorioEvidencias;
    @MockitoBean RepositorioUbicaciones repositorioUbicaciones;
    @MockitoBean AlmacenArchivos almacen;
    @MockitoBean RepositorioCambios repositorioCambios;
    @MockitoBean ConsultaHallazgos consultaHallazgos;
    @MockitoBean PlatformTransactionManager transacciones;

    private TipoEspecie anfibio;
    private Hallazgo hallazgo;
    private Usuario carolina;
    private final List<FichaTaxonomica> fichas = new ArrayList<>();
    private final List<RegistroCambio> cambios = new ArrayList<>();

    @BeforeEach
    void preparar() {
        Usuario laura = usuario("Laura Gómez", "investigador1@biohub.test", rol(INVESTIGADOR, "REGISTRAR_HALLAZGO"));
        carolina = usuario("Carolina Vélez", "auditor@biohub.test",
                rol(AUDITOR, "RECLASIFICAR_HALLAZGO", "VER_HISTORIAL", "VER_INTENTOS"));
        List<Usuario> usuarios = List.of(laura, carolina);
        when(repositorioUsuarios.buscarPorCorreo(anyString())).thenAnswer(i -> usuarios.stream()
                .filter(u -> u.getCorreo().equals(i.getArgument(0))).findFirst());
        when(repositorioUsuarios.buscarPorId(any(UUID.class))).thenAnswer(i -> usuarios.stream()
                .filter(u -> u.getId().equals(i.getArgument(0))).findFirst());

        anfibio = TaxonomiaDePrueba.anfibio();
        when(repositorioTaxonomia.buscarTipo(anfibio.getId())).thenReturn(Optional.of(anfibio));
        when(repositorioTaxonomia.listarTipos()).thenReturn(List.of(anfibio));

        hallazgo = new Hallazgo("Expedición Atrato 2026", laura.getId(), REGISTRO);
        when(repositorioHallazgos.buscarPorId(hallazgo.getId())).thenReturn(Optional.of(hallazgo));

        // Las fichas y los cambios viven en listas, como si fueran las tablas
        fichas.clear();
        cambios.clear();
        fichas.add(fichaOriginal(anfibio, hallazgo.getId()));
        doAnswer(i -> fichas.add(conId(i.getArgument(0)))).when(repositorioFichas).guardar(any());
        when(repositorioFichas.buscarVigente(any())).thenAnswer(i -> Optional.of(fichas.getLast()));
        when(repositorioFichas.listarVersiones(any())).thenAnswer(i -> List.copyOf(fichas));
        doAnswer(i -> cambios.add(i.getArgument(0))).when(repositorioCambios).guardar(any());
        when(repositorioCambios.listarPorHallazgo(any())).thenAnswer(i -> List.copyOf(cambios));
    }

    private String tokenDe(String correo) throws Exception {
        String json = mvc.perform(post("/interno/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private String reclasificacion(String justificacion) {
        return """
                {
                  "justificacion": %s,
                  "ficha": {
                    "tipoEspecieId": "%s",
                    "reino": "Animalia", "filo": "Chordata", "clase": "Amphibia", "orden": "Anura",
                    "familia": "Dendrobatidae", "genero": "Oophaga", "especie": "histrionica",
                    "valores": { "%s": "24.5" }
                  }
                }
                """.formatted(justificacion == null ? "null" : "\"" + justificacion + "\"",
                anfibio.getId(), idDe(anfibio, "temperatura_agua_c"));
    }

    private String rutaReclasificar() {
        return "/interno/hallazgos/" + hallazgo.getId() + "/reclasificaciones";
    }

    @Test
    void elAuditorReclasificaYQuedaLaVersionNuevaConSuRegistro() throws Exception {
        mvc.perform(post(rutaReclasificar())
                        .header("Authorization", "Bearer " + tokenDe("auditor@biohub.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reclasificacion(JUSTIFICACION)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionAnterior").value(1))
                .andExpect(jsonPath("$.versionNueva").value(2))
                .andExpect(jsonPath("$.especieAnterior").value("Dendrobates histrionicus"))
                .andExpect(jsonPath("$.especieNueva").value("Oophaga histrionica"))
                .andExpect(jsonPath("$.autorizadoPor").value("Carolina Vélez"));

        assertThat(fichas).hasSize(2);
        assertThat(cambios).singleElement().satisfies(c -> {
            assertThat(c.getAutorizadoPor()).isEqualTo(carolina.getId());
            assertThat(c.getVersionNuevaId()).isEqualTo(fichas.get(1).getId());
        });
    }

    @Test
    void sinJustificacionResponde400YNoGuardaNada() throws Exception {
        mvc.perform(post(rutaReclasificar())
                        .header("Authorization", "Bearer " + tokenDe("auditor@biohub.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reclasificacion(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("Falta la justificación del cambio."));

        assertThat(fichas).hasSize(1);
        verify(repositorioCambios, never()).guardar(any());
    }

    @Test
    void siOtroAuditorGanoLaVersionResponde409() throws Exception {
        doThrow(new VersionDuplicadaException()).when(repositorioCambios).guardar(any());

        mvc.perform(post(rutaReclasificar())
                        .header("Authorization", "Bearer " + tokenDe("auditor@biohub.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reclasificacion(JUSTIFICACION)))
                .andExpect(status().isConflict());
    }

    @Test
    void unInvestigadorNoPuedeReclasificarYQuedaElIntento() throws Exception {
        mvc.perform(post(rutaReclasificar())
                        .header("Authorization", "Bearer " + tokenDe("investigador1@biohub.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reclasificacion(JUSTIFICACION)))
                .andExpect(status().isForbidden());

        assertThat(fichas).hasSize(1);
        ArgumentCaptor<IntentoAccesoNoAutorizado> intento = ArgumentCaptor.forClass(IntentoAccesoNoAutorizado.class);
        verify(repositorioIntentos).guardar(intento.capture());
        assertThat(intento.getValue().getRecursoSolicitado()).startsWith("POST /interno/hallazgos/");
    }

    @Test
    void reclasificarUnHallazgoQueNoExisteResponde404() throws Exception {
        mvc.perform(post("/interno/hallazgos/" + UUID.randomUUID() + "/reclasificaciones")
                        .header("Authorization", "Bearer " + tokenDe("auditor@biohub.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reclasificacion(JUSTIFICACION)))
                .andExpect(status().isNotFound());
    }

    @Test
    void elHistorialMuestraLasVersionesEnOrdenConQuienAutorizo() throws Exception {
        String token = tokenDe("auditor@biohub.test");
        mvc.perform(post(rutaReclasificar())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reclasificacion(JUSTIFICACION)))
                .andExpect(status().isCreated());

        mvc.perform(get("/interno/historial/hallazgos/" + hallazgo.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.investigador").value("Laura Gómez"))
                .andExpect(jsonPath("$.versionVigente").value(2))
                .andExpect(jsonPath("$.versiones.length()").value(2))
                .andExpect(jsonPath("$.versiones[0].numeroVersion").value(1))
                .andExpect(jsonPath("$.versiones[0].genero").value("Dendrobates"))
                .andExpect(jsonPath("$.versiones[0].vigente").value(false))
                .andExpect(jsonPath("$.versiones[0].cambio").doesNotExist())
                .andExpect(jsonPath("$.versiones[1].genero").value("Oophaga"))
                .andExpect(jsonPath("$.versiones[1].vigente").value(true))
                .andExpect(jsonPath("$.versiones[1].cambio.autorizadoPor").value("Carolina Vélez"))
                .andExpect(jsonPath("$.versiones[1].cambio.justificacion").value(JUSTIFICACION));
    }

    @Test
    void elAuditorVeLaListaConLaVersionVigente() throws Exception {
        when(consultaHallazgos.listarConVersionVigente()).thenReturn(List.of(new ResumenHallazgo(
                hallazgo.getId(), REGISTRO, "Expedición Atrato 2026", "Laura Gómez", "Anfibio",
                "Oophaga histrionica", "Dendrobatidae", "Chocó", 2)));

        mvc.perform(get("/interno/historial/hallazgos")
                        .header("Authorization", "Bearer " + tokenDe("auditor@biohub.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].especie").value("Oophaga histrionica"))
                .andExpect(jsonPath("$[0].versionVigente").value(2));
    }

    @Test
    void unInvestigadorNoPuedeVerElHistorial() throws Exception {
        mvc.perform(get("/interno/historial/hallazgos/" + hallazgo.getId())
                        .header("Authorization", "Bearer " + tokenDe("investigador1@biohub.test")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAuditorPuedeLeerLosTiposDeEspecieParaReclasificar() throws Exception {
        mvc.perform(get("/interno/tipos-especie")
                        .header("Authorization", "Bearer " + tokenDe("auditor@biohub.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Anfibio"));
    }

    @Test
    void sinLoginNoSePuedeVerNada() throws Exception {
        mvc.perform(get("/interno/historial/hallazgos")).andExpect(status().isUnauthorized());
    }
}
