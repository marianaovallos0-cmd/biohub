package co.edu.unbosque.biohub.interna.seguridad.web;

import co.edu.unbosque.biohub.interna.seguridad.dominio.IntentoAccesoNoAutorizado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioIntentos;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioUsuarios;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.CLAVE;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.rol;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.usuario;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.AUDITOR;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.INVESTIGADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// HU-14 de punta a punta con el perfil interna. Los repositorios son dobles, no hace falta PostgreSQL.
@SpringBootTest(properties = {
        "biohub.jwt.secreto=clave-solo-para-las-pruebas-de-biohub-2026",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("interna")
class IntentosControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RepositorioUsuarios repositorioUsuarios;

    @MockitoBean
    RepositorioIntentos repositorioIntentos;

    // registrarIntento abre su propia transacción y sin PostgreSQL no hay conexión para abrirla
    @MockitoBean
    PlatformTransactionManager transacciones;

    private Usuario camila;
    private Usuario laura;

    @BeforeEach
    void preparar() {
        camila = usuario("Camila Díaz", "auditor@biohub.test", rol(AUDITOR, "VER_HISTORIAL", "VER_INTENTOS"));
        laura = usuario("Laura Gómez", "investigador1@biohub.test", rol(INVESTIGADOR, "REGISTRAR_HALLAZGO"));
        List<Usuario> usuarios = List.of(camila, laura);

        when(repositorioUsuarios.buscarPorCorreo(anyString())).thenAnswer(i -> usuarios.stream()
                .filter(u -> u.getCorreo().equals(i.getArgument(0))).findFirst());
        when(repositorioUsuarios.buscarPorId(any(UUID.class))).thenAnswer(i -> usuarios.stream()
                .filter(u -> u.getId().equals(i.getArgument(0))).findFirst());
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
    void elAuditorConsultaLosIntentosDelPeriodo() throws Exception {
        when(repositorioIntentos.buscarEntre(any(), any())).thenReturn(List.of(new IntentoAccesoNoAutorizado(
                laura, "GET /interno/intentos", "10.0.0.7", OffsetDateTime.parse("2026-11-12T15:00-05:00"))));
        String token = iniciarSesion("auditor@biohub.test");

        mvc.perform(get("/interno/intentos").param("desde", "2026-11-01").param("hasta", "2026-11-15")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").value("investigador1@biohub.test"))
                .andExpect(jsonPath("$[0].recurso").value("GET /interno/intentos"));

        // El 15 entra completo: se busca hasta el 16 a las 00:00 en hora de Colombia
        verify(repositorioIntentos).buscarEntre(
                OffsetDateTime.parse("2026-11-01T00:00-05:00"),
                OffsetDateTime.parse("2026-11-16T00:00-05:00"));
    }

    @Test
    void unInvestigadorNoPuedeVerLosIntentosYSuIntentoQuedaGuardado() throws Exception {
        String token = iniciarSesion("investigador1@biohub.test");

        mvc.perform(get("/interno/intentos").param("desde", "2026-11-01").param("hasta", "2026-11-15")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        ArgumentCaptor<IntentoAccesoNoAutorizado> guardado = ArgumentCaptor.forClass(IntentoAccesoNoAutorizado.class);
        verify(repositorioIntentos).guardar(guardado.capture());
        assertThat(guardado.getValue().getUsuario()).isSameAs(laura);
        assertThat(guardado.getValue().getRecursoSolicitado()).isEqualTo("GET /interno/intentos");
        assertThat(guardado.getValue().getFechaHora()).isNotNull();
        verify(repositorioIntentos, never()).buscarEntre(any(), any());
    }

    @Test
    void sinSesionNoSeGuardaIntentoPorqueNoSeSabeQuienEs() throws Exception {
        mvc.perform(get("/interno/intentos").param("desde", "2026-11-01").param("hasta", "2026-11-15"))
                .andExpect(status().isUnauthorized());

        verify(repositorioIntentos, never()).guardar(any());
    }

    @Test
    void conElRangoAlRevesResponde400() throws Exception {
        String token = iniciarSesion("auditor@biohub.test");

        mvc.perform(get("/interno/intentos").param("desde", "2026-11-15").param("hasta", "2026-11-01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La fecha inicial no puede ser posterior a la fecha final."));
    }
}
