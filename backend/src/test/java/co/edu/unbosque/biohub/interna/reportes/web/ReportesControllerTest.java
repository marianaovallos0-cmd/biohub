package co.edu.unbosque.biohub.interna.reportes.web;

import co.edu.unbosque.biohub.interna.reportes.dominio.FilaReporte;
import co.edu.unbosque.biohub.interna.reportes.dominio.Reporte;
import co.edu.unbosque.biohub.interna.reportes.dominio.RepositorioReportes;
import co.edu.unbosque.biohub.interna.seguridad.dominio.IntentoAccesoNoAutorizado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioIntentos;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioUsuarios;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.CLAVE;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.rol;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.usuario;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.DIRECTOR;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.INVESTIGADOR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// HU-18 de punta a punta con el perfil interna. El repositorio es un doble, no hace falta PostgreSQL.
@SpringBootTest(properties = {
        "biohub.jwt.secreto=clave-solo-para-las-pruebas-de-biohub-2026",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("interna")
class ReportesControllerTest {

    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RepositorioUsuarios repositorioUsuarios;

    @MockitoBean
    RepositorioIntentos repositorioIntentos;

    @MockitoBean
    RepositorioReportes repositorioReportes;

    @MockitoBean
    PlatformTransactionManager transacciones;

    private Usuario jorge;

    @BeforeEach
    void preparar() {
        jorge = usuario("Jorge Peña", "director@biohub.test", rol(DIRECTOR, "VER_REPORTES"));
        Usuario laura = usuario("Laura Gómez", "investigador1@biohub.test", rol(INVESTIGADOR, "REGISTRAR_HALLAZGO"));
        List<Usuario> usuarios = List.of(jorge, laura);

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
    void elDirectorPideUnMesTerminadoYQuedaPendiente() throws Exception {
        Reporte pendiente = new Reporte(UUID.randomUUID(), OCTUBRE, null);
        when(repositorioReportes.buscarPorMes(OCTUBRE)).thenReturn(Optional.empty());
        when(repositorioReportes.crearPendiente(OCTUBRE, jorge.getId())).thenReturn(pendiente);
        when(repositorioReportes.buscarPorId(pendiente.id())).thenReturn(Optional.of(pendiente));
        when(repositorioReportes.filas(pendiente.id())).thenReturn(List.of());
        String token = iniciarSesion("director@biohub.test");

        mvc.perform(post("/interno/reportes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mes\":\"2026-10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2026-10"))
                .andExpect(jsonPath("$.generado").value(false))
                .andExpect(jsonPath("$.totalHallazgos").value(0));

        verify(repositorioReportes).crearPendiente(OCTUBRE, jorge.getId());
    }

    @Test
    void unMesQueNoHaTerminadoResponde400() throws Exception {
        String token = iniciarSesion("director@biohub.test");

        mvc.perform(post("/interno/reportes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mes\":\"2999-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Solo se pueden pedir reportes de meses que ya terminaron."));

        verify(repositorioReportes, never()).crearPendiente(any(), any());
    }

    // Criterio 3: el reporte ya generado se lee tal cual, sin recalcular
    @Test
    void elDirectorConsultaUnReporteGenerado() throws Exception {
        Reporte octubre = new Reporte(UUID.randomUUID(), OCTUBRE, OffsetDateTime.parse("2026-11-01T01:00-05:00"));
        when(repositorioReportes.buscarPorId(octubre.id())).thenReturn(Optional.of(octubre));
        when(repositorioReportes.filas(octubre.id())).thenReturn(List.of(
                new FilaReporte("Amazonas", 2, 0, new BigDecimal("100.00"), false),
                new FilaReporte("Chocó", 0, 0, new BigDecimal("-100.00"), true)));
        String token = iniciarSesion("director@biohub.test");

        mvc.perform(get("/interno/reportes/" + octubre.id()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generado").value(true))
                .andExpect(jsonPath("$.totalHallazgos").value(2))
                .andExpect(jsonPath("$.departamentosEnAlerta").value(1))
                .andExpect(jsonPath("$.filas.length()").value(2))
                .andExpect(jsonPath("$.filas[1].departamento").value("Chocó"));

        verify(repositorioReportes, never()).crearPendiente(any(), any());
    }

    @Test
    void unReporteQueNoExisteResponde404() throws Exception {
        UUID id = UUID.randomUUID();
        when(repositorioReportes.buscarPorId(id)).thenReturn(Optional.empty());
        String token = iniciarSesion("director@biohub.test");

        mvc.perform(get("/interno/reportes/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void unInvestigadorNoVeLosReportesYSuIntentoQuedaGuardado() throws Exception {
        String token = iniciarSesion("investigador1@biohub.test");

        mvc.perform(get("/interno/reportes").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verify(repositorioIntentos).guardar(any(IntentoAccesoNoAutorizado.class));
        verify(repositorioReportes, never()).listar();
    }
}
