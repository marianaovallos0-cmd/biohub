package co.edu.unbosque.biohub.interna.seguridad.web;

import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioIntentos;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioUsuarios;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.CLAVE;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.rol;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.usuario;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.AUDITOR;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.INVESTIGADOR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración de la HU-12 con el perfil interna: login, token, permisos y rechazos.
 * Solo el repositorio es un doble, para no necesitar PostgreSQL.
 */
@SpringBootTest(properties = {
        "biohub.jwt.secreto=clave-solo-para-las-pruebas-de-biohub-2026",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("interna")
@Import(SeguridadInternaTest.EndpointDePrueba.class)
class SeguridadInternaTest {

    /** Un endpoint que solo existe en esta prueba y pide VER_INTENTOS, como lo haría uno real. */
    @RestController
    public static class EndpointDePrueba {
        @PreAuthorize("@autorizacion.validarAcceso(principal, 'VER_INTENTOS')")
        @GetMapping("/interno/prueba/intentos")
        public String intentos() {
            return "ok";
        }
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RepositorioUsuarios repositorio;

    @MockitoBean
    RepositorioIntentos repositorioIntentos;

    private Usuario camila;
    private Usuario laura;

    @BeforeEach
    void preparar() {
        camila = usuario("Camila Díaz", "auditor@biohub.test", rol(AUDITOR, "VER_HISTORIAL", "VER_INTENTOS"));
        laura = usuario("Laura Gómez", "investigador1@biohub.test", rol(INVESTIGADOR, "REGISTRAR_HALLAZGO"));
        List<Usuario> usuarios = List.of(camila, laura);

        when(repositorio.buscarPorCorreo(anyString())).thenAnswer(i -> usuarios.stream()
                .filter(u -> u.getCorreo().equals(i.getArgument(0))).findFirst());
        when(repositorio.buscarPorId(any(UUID.class))).thenAnswer(i -> usuarios.stream()
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
    void elLoginCorrectoDevuelveTokenYElRolDelUsuario() throws Exception {
        mvc.perform(post("/interno/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"auditor@biohub.test\",\"contrasena\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.rol").value("AUDITOR"));
    }

    @Test
    void elLoginConClaveIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/interno/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"auditor@biohub.test\",\"contrasena\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/interno/auth/yo")).andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenDevuelveQuienEsYSusPermisos() throws Exception {
        String token = iniciarSesion("auditor@biohub.test");

        mvc.perform(get("/interno/auth/yo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Camila Díaz"))
                .andExpect(jsonPath("$.rol").value("AUDITOR"))
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
    }

    @Test
    void unRolConElPermisoEntra() throws Exception {
        String token = iniciarSesion("auditor@biohub.test");

        mvc.perform(get("/interno/prueba/intentos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void unRolSinElPermisoRecibe403() throws Exception {
        String token = iniciarSesion("investigador1@biohub.test");

        mvc.perform(get("/interno/prueba/intentos").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Tu rol no tiene permiso para esta acción."));
    }

    @Test
    void siLeCambianElRolAplicaConElMismoToken() throws Exception {
        String token = iniciarSesion("investigador1@biohub.test");
        mvc.perform(get("/interno/prueba/intentos").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        laura.cambiarRol(rol(AUDITOR, "VER_INTENTOS"));

        mvc.perform(get("/interno/prueba/intentos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void siDesactivanAlUsuarioSuTokenDejaDeServir() throws Exception {
        String token = iniciarSesion("auditor@biohub.test");

        camila.desactivar();

        mvc.perform(get("/interno/auth/yo").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}