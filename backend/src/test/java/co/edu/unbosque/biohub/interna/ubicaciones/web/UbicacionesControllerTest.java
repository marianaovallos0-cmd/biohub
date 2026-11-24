package co.edu.unbosque.biohub.interna.ubicaciones.web;

import co.edu.unbosque.biohub.interna.seguridad.dominio.IntentoAccesoNoAutorizado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioIntentos;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioUsuarios;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.RepositorioConsultaUbicaciones;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.UbicacionConsultada;
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
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.ALTO_RANGO;
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

// HU-13 de punta a punta con el perfil interna. Los repositorios son dobles, no hace falta PostgreSQL.
@SpringBootTest(properties = {
        "biohub.jwt.secreto=clave-solo-para-las-pruebas-de-biohub-2026",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("interna")
class UbicacionesControllerTest {

    private static final UUID CONDOR = UUID.fromString("40000000-0000-0000-0000-000000000009");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RepositorioUsuarios repositorioUsuarios;

    @MockitoBean
    RepositorioIntentos repositorioIntentos;

    @MockitoBean
    RepositorioConsultaUbicaciones ubicaciones;

    // registrarIntento abre su propia transacción y sin PostgreSQL no hay conexión para abrirla
    @MockitoBean
    PlatformTransactionManager transacciones;

    @BeforeEach
    void preparar() {
        Usuario sofia = usuario("Sofía Herrera", "altorango@biohub.test", rol(ALTO_RANGO, "VER_COORDENADAS"));
        Usuario laura = usuario("Laura Gómez", "investigador1@biohub.test", rol(INVESTIGADOR, "REGISTRAR_HALLAZGO"));
        Usuario camila = usuario("Camila Díaz", "auditor@biohub.test", rol(AUDITOR, "VER_HISTORIAL", "VER_INTENTOS"));
        List<Usuario> usuarios = List.of(sofia, laura, camila);

        when(repositorioUsuarios.buscarPorCorreo(anyString())).thenAnswer(i -> usuarios.stream()
                .filter(u -> u.getCorreo().equals(i.getArgument(0))).findFirst());
        when(repositorioUsuarios.buscarPorId(any(UUID.class))).thenAnswer(i -> usuarios.stream()
                .filter(u -> u.getId().equals(i.getArgument(0))).findFirst());

        when(ubicaciones.exacta(CONDOR)).thenReturn(Optional.of(
                UbicacionConsultada.exacta(CONDOR, "Vultur gryphus", "Andina", "Boyacá", true, 6.0021, -72.3047)));
        when(ubicaciones.publica(CONDOR)).thenReturn(Optional.of(
                UbicacionConsultada.publica(CONDOR, "Vultur gryphus", "Andina", "Boyacá", true)));
        when(ubicaciones.todasExactas()).thenReturn(List.of(
                UbicacionConsultada.exacta(CONDOR, "Vultur gryphus", "Andina", "Boyacá", true, 6.0021, -72.3047)));
        when(ubicaciones.todasPublicas()).thenReturn(List.of(
                UbicacionConsultada.publica(CONDOR, "Vultur gryphus", "Andina", "Boyacá", true)));
    }

    private String iniciarSesion(String correo) throws Exception {
        String json = mvc.perform(post("/interno/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private String pedir(String ruta, String correo) throws Exception {
        return mvc.perform(get(ruta).header("Authorization", "Bearer " + iniciarSesion(correo)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // ---------- Criterio 1 ----------

    @Test
    void elAltoRangoVeLasCoordenadasExactas() throws Exception {
        mvc.perform(get("/interno/hallazgos/{id}/ubicacion", CONDOR)
                        .header("Authorization", "Bearer " + iniciarSesion("altorango@biohub.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exacta").value(true))
                .andExpect(jsonPath("$.latitud").value(6.0021))
                .andExpect(jsonPath("$.longitud").value(-72.3047))
                .andExpect(jsonPath("$.departamento").value("Boyacá"));

        verify(repositorioIntentos, never()).guardar(any());
    }

    // ---------- Criterios 2 y 4 ----------

    @Test
    void otroRolSoloVeRegionYDepartamentoYNoHayNiRastroDeCoordenadas() throws Exception {
        String json = pedir("/interno/hallazgos/" + CONDOR + "/ubicacion", "investigador1@biohub.test");

        assertThat(JsonPath.<Boolean>read(json, "$.exacta")).isFalse();
        assertThat(JsonPath.<String>read(json, "$.region")).isEqualTo("Andina");
        assertThat(json.toLowerCase()).doesNotContain("latitud", "longitud", "6.00", "-72.");
        verify(ubicaciones, never()).exacta(any());
    }

    @Test
    void laListaDelAuditorTampocoTraeCoordenadas() throws Exception {
        String json = pedir("/interno/ubicaciones", "auditor@biohub.test");

        assertThat(json.toLowerCase()).doesNotContain("latitud", "longitud");
        verify(ubicaciones, never()).todasExactas();
    }

    @Test
    void laListaDelAltoRangoTraeCoordenadas() throws Exception {
        mvc.perform(get("/interno/ubicaciones")
                        .header("Authorization", "Bearer " + iniciarSesion("altorango@biohub.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].latitud").value(6.0021));
    }

    @Test
    void cuandoLaValidacionFallaElIntentoQuedaRegistrado() throws Exception {
        pedir("/interno/hallazgos/" + CONDOR + "/ubicacion", "investigador1@biohub.test");

        ArgumentCaptor<IntentoAccesoNoAutorizado> intento = ArgumentCaptor.forClass(IntentoAccesoNoAutorizado.class);
        verify(repositorioIntentos).guardar(intento.capture());
        assertThat(intento.getValue().getRecursoSolicitado())
                .isEqualTo("GET /interno/hallazgos/" + CONDOR + "/ubicacion");
        assertThat(intento.getValue().getUsuario().getCorreo()).isEqualTo("investigador1@biohub.test");
    }

    // ---------- Criterio 3: sin sesión no hay nada ----------

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/interno/hallazgos/{id}/ubicacion", CONDOR)).andExpect(status().isUnauthorized());
        mvc.perform(get("/interno/ubicaciones")).andExpect(status().isUnauthorized());
        verify(ubicaciones, never()).exacta(any());
        verify(ubicaciones, never()).publica(any());
    }

    @Test
    void unHallazgoSinUbicacionResponde404() throws Exception {
        UUID otro = UUID.randomUUID();
        when(ubicaciones.exacta(otro)).thenReturn(Optional.empty());

        mvc.perform(get("/interno/hallazgos/{id}/ubicacion", otro)
                        .header("Authorization", "Bearer " + iniciarSesion("altorango@biohub.test")))
                .andExpect(status().isNotFound());
    }
}
