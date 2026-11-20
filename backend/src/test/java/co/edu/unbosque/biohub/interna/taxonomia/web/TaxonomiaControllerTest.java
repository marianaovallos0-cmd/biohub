package co.edu.unbosque.biohub.interna.taxonomia.web;

import co.edu.unbosque.biohub.interna.seguridad.dominio.IntentoAccesoNoAutorizado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioIntentos;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioUsuarios;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.RepositorioFichas;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.RepositorioTaxonomia;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import org.springframework.test.util.ReflectionTestUtils;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.CLAVE;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.rol;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.usuario;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.ADMINISTRADOR;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.INVESTIGADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// HU-02 de punta a punta con el perfil interna. Los repositorios son dobles, no hace falta PostgreSQL.
@SpringBootTest(properties = {
        "biohub.jwt.secreto=clave-solo-para-las-pruebas-de-biohub-2026",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("interna")
class TaxonomiaControllerTest {

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
    PlatformTransactionManager transacciones;

    private Usuario admin;

    @BeforeEach
    void preparar() {
        Usuario laura = usuario("Laura Gómez", "investigador1@biohub.test",
                rol(INVESTIGADOR, "REGISTRAR_HALLAZGO", "GESTIONAR_TAXONOMIA"));
        admin = usuario("Admin BioHub", "admin@biohub.test", rol(ADMINISTRADOR, "GESTIONAR_EQUIPOS"));
        List<Usuario> usuarios = List.of(laura, admin);

        when(repositorioUsuarios.buscarPorCorreo(anyString())).thenAnswer(i -> usuarios.stream()
                .filter(u -> u.getCorreo().equals(i.getArgument(0))).findFirst());
        when(repositorioUsuarios.buscarPorId(any(UUID.class))).thenAnswer(i -> usuarios.stream()
                .filter(u -> u.getId().equals(i.getArgument(0))).findFirst());
        when(repositorioTaxonomia.listarTipos())
                .thenReturn(List.of(TaxonomiaDePrueba.anfibio(), TaxonomiaDePrueba.ave()));
    }

    private String iniciarSesion(String correo) throws Exception {
        String json = mvc.perform(post("/interno/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    @Test
    void elInvestigadorRecibeCadaTipoConSusAtributos() throws Exception {
        String token = iniciarSesion("investigador1@biohub.test");

        mvc.perform(get("/interno/tipos-especie").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Anfibio"))
                .andExpect(jsonPath("$[0].atributos[0].nombre").value("temperatura_agua_c"))
                .andExpect(jsonPath("$[0].atributos[0].tipoDato").value("NUMERO"))
                .andExpect(jsonPath("$[0].atributos[0].obligatorio").value(true))
                .andExpect(jsonPath("$[1].nombre").value("Ave"))
                .andExpect(jsonPath("$[1].atributos[0].nombre").value("ruta_gps"));
    }

    @Test
    void unRolSinPermisoRecibe403YSuIntentoQuedaGuardado() throws Exception {
        String token = iniciarSesion("admin@biohub.test");

        mvc.perform(get("/interno/tipos-especie").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        ArgumentCaptor<IntentoAccesoNoAutorizado> guardado = ArgumentCaptor.forClass(IntentoAccesoNoAutorizado.class);
        verify(repositorioIntentos).guardar(guardado.capture());
        assertThat(guardado.getValue().getUsuario()).isSameAs(admin);
        assertThat(guardado.getValue().getRecursoSolicitado()).isEqualTo("GET /interno/tipos-especie");
    }

    // HU-03 de punta a punta: el investigador agrega un atributo y el siguiente GET ya lo trae
    @Test
    void elInvestigadorAgregaUnAtributoYApareceEnElFormulario() throws Exception {
        TipoEspecie anfibio = TaxonomiaDePrueba.anfibio();
        when(repositorioTaxonomia.buscarTipo(anfibio.getId())).thenReturn(Optional.of(anfibio));
        when(repositorioTaxonomia.listarTipos()).thenReturn(List.of(anfibio));
        doAnswer(i -> {
            // Se guarda en una variable Object: si no, Java escoge la versión de setField que recibe una Class
            Object definicion = i.getArgument(0);
            ReflectionTestUtils.setField(definicion, "id", UUID.randomUUID());
            return null;
        }).when(repositorioTaxonomia).guardarDefinicion(any());
        String token = iniciarSesion("investigador1@biohub.test");

        mvc.perform(post("/interno/tipos-especie/" + anfibio.getId() + "/atributos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Color de piel\",\"tipoDato\":\"TEXTO\",\"obligatorio\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("color_de_piel"))
                .andExpect(jsonPath("$.tipoDato").value("TEXTO"));

        mvc.perform(get("/interno/tipos-especie").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].atributos[?(@.nombre == 'color_de_piel')]").exists());
    }

    @Test
    void unAtributoRepetidoResponde400() throws Exception {
        TipoEspecie anfibio = TaxonomiaDePrueba.anfibio();
        when(repositorioTaxonomia.buscarTipo(anfibio.getId())).thenReturn(Optional.of(anfibio));
        String token = iniciarSesion("investigador1@biohub.test");

        mvc.perform(post("/interno/tipos-especie/" + anfibio.getId() + "/atributos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"temperatura_agua_c\",\"tipoDato\":\"NUMERO\",\"obligatorio\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El tipo «Anfibio» ya tiene el atributo «temperatura_agua_c»."));
        verify(repositorioTaxonomia, never()).guardarDefinicion(any());
    }

    @Test
    void unRolSinGestionarTaxonomiaNoAgregaAtributosYSuIntentoQuedaGuardado() throws Exception {
        String token = iniciarSesion("admin@biohub.test");

        mvc.perform(post("/interno/tipos-especie/" + UUID.randomUUID() + "/atributos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"ph\",\"tipoDato\":\"NUMERO\",\"obligatorio\":false}"))
                .andExpect(status().isForbidden());

        verify(repositorioIntentos).guardar(any(IntentoAccesoNoAutorizado.class));
        verify(repositorioTaxonomia, never()).guardarDefinicion(any());
    }

    @Test
    void sinSesionResponde401() throws Exception {
        mvc.perform(get("/interno/tipos-especie")).andExpect(status().isUnauthorized());
    }
}
