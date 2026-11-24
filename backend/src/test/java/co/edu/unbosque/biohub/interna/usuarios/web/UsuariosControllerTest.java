package co.edu.unbosque.biohub.interna.usuarios.web;

import co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba;
import co.edu.unbosque.biohub.interna.seguridad.dominio.IntentoAccesoNoAutorizado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioIntentos;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import co.edu.unbosque.biohub.interna.usuarios.AlmacenEnMemoria;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-15 de punta a punta con el perfil interna: login, token, permisos, controlador y casos de uso.
 * Los datos viven en AlmacenEnMemoria, compartido entre el login y la gestión, como la base real.
 */
@SpringBootTest(properties = {
        "biohub.jwt.secreto=clave-solo-para-las-pruebas-de-biohub-2026",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("interna")
@Import({UsuariosControllerTest.Almacen.class, UsuariosControllerTest.EndpointsDePrueba.class})
class UsuariosControllerTest {

    @TestConfiguration
    static class Almacen {
        @Bean
        @Primary
        AlmacenEnMemoria almacenEnMemoria() {
            return new AlmacenEnMemoria();
        }
    }

    /** Endpoints que solo existen en esta prueba, para comprobar qué permisos tiene cada usuario. */
    @RestController
    public static class EndpointsDePrueba {
        @PreAuthorize("@autorizacion.validarAcceso(principal, 'REGISTRAR_HALLAZGO')")
        @GetMapping("/interno/prueba/registrar")
        public String registrar() {
            return "ok";
        }

        @PreAuthorize("@autorizacion.validarAcceso(principal, 'VER_HISTORIAL')")
        @GetMapping("/interno/prueba/historial")
        public String historial() {
            return "ok";
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    AlmacenEnMemoria almacen;

    @MockitoBean
    RepositorioIntentos repositorioIntentos;

    // Los casos de uso son @Transactional y sin PostgreSQL no hay conexión para abrir la transacción
    @MockitoBean
    PlatformTransactionManager transacciones;

    private Usuario laura;
    private Usuario andres;
    private String tokenAdmin;

    @BeforeEach
    void preparar() throws Exception {
        almacen.reiniciar();
        almacen.agregarUsuario("Admin BioHub", "admin@biohub.test", almacen.administrador);
        laura = almacen.agregarUsuario("Laura Gómez", "investigador1@biohub.test", almacen.investigador);
        andres = almacen.agregarUsuario("Andrés Rojas", "investigador2@biohub.test", almacen.investigador);
        tokenAdmin = iniciarSesion("admin@biohub.test", DatosDePrueba.CLAVE);
    }

    private String iniciarSesion(String correo, String contrasena) throws Exception {
        String json = mvc.perform(post("/interno/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private static MockHttpServletRequestBuilder con(MockHttpServletRequestBuilder peticion, String token) {
        return peticion.header("Authorization", "Bearer " + token);
    }

    private String crearUsuarioJson(String rolId) {
        String rol = rolId == null ? "null" : "\"" + rolId + "\"";
        return "{\"nombre\":\"Pedro Ruiz\",\"correo\":\"pedro@biohub.test\","
                + "\"contrasena\":\"clave-de-pedro\",\"rolId\":" + rol + "}";
    }

    // ---------- Criterio 1: crear con rol deja al usuario habilitado con esos permisos ----------

    @Test
    void elAdministradorCreaUnUsuarioQuePuedeEntrarConLosPermisosDeSuRol() throws Exception {
        mvc.perform(con(post("/interno/usuarios"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearUsuarioJson(almacen.investigador.getId().toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("pedro@biohub.test"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.nivel").value("INVESTIGADOR"))
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());

        String tokenPedro = iniciarSesion("pedro@biohub.test", "clave-de-pedro");
        mvc.perform(con(get("/interno/prueba/registrar"), tokenPedro)).andExpect(status().isOk());
        mvc.perform(con(get("/interno/prueba/historial"), tokenPedro)).andExpect(status().isForbidden());
    }

    // ---------- Criterio 2: cambiar los permisos del rol aplica a todos los que lo tienen ----------

    @Test
    void agregarYQuitarUnPermisoAlRolAplicaATodosSinVolverAEntrar() throws Exception {
        String tokenLaura = iniciarSesion("investigador1@biohub.test", DatosDePrueba.CLAVE);
        String tokenAndres = iniciarSesion("investigador2@biohub.test", DatosDePrueba.CLAVE);
        mvc.perform(con(get("/interno/prueba/historial"), tokenLaura)).andExpect(status().isForbidden());

        String rol = almacen.investigador.getId().toString();
        mvc.perform(con(put("/interno/roles/{id}/permisos/{codigo}", rol, "VER_HISTORIAL"), tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permisos").value(org.hamcrest.Matchers.hasItem("VER_HISTORIAL")));

        // Con los mismos tokens de antes: el rol se lee de la base en cada petición
        mvc.perform(con(get("/interno/prueba/historial"), tokenLaura)).andExpect(status().isOk());
        mvc.perform(con(get("/interno/prueba/historial"), tokenAndres)).andExpect(status().isOk());

        mvc.perform(con(delete("/interno/roles/{id}/permisos/{codigo}", rol, "VER_HISTORIAL"), tokenAdmin))
                .andExpect(status().isOk());

        mvc.perform(con(get("/interno/prueba/historial"), tokenLaura)).andExpect(status().isForbidden());
        mvc.perform(con(get("/interno/prueba/historial"), tokenAndres)).andExpect(status().isForbidden());
    }

    // ---------- Criterio 3: sin rol no se guarda ----------

    @Test
    void unUsuarioSinRolNoSeGuarda() throws Exception {
        mvc.perform(con(post("/interno/usuarios"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearUsuarioJson(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El usuario debe tener un rol."));

        assertThat(almacen.buscarPorCorreo("pedro@biohub.test")).isEmpty();
    }

    // ---------- Solo el administrador entra ----------

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/interno/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void unInvestigadorNoPuedeGestionarUsuariosYSuIntentoQuedaGuardado() throws Exception {
        String tokenLaura = iniciarSesion("investigador1@biohub.test", DatosDePrueba.CLAVE);

        mvc.perform(con(post("/interno/usuarios"), tokenLaura)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearUsuarioJson(almacen.administrador.getId().toString())))
                .andExpect(status().isForbidden());

        assertThat(almacen.buscarPorCorreo("pedro@biohub.test")).isEmpty();
        ArgumentCaptor<IntentoAccesoNoAutorizado> intento = ArgumentCaptor.forClass(IntentoAccesoNoAutorizado.class);
        verify(repositorioIntentos).guardar(intento.capture());
        assertThat(intento.getValue().getRecursoSolicitado()).isEqualTo("POST /interno/usuarios");
    }

    // ---------- Activar y desactivar ----------

    @Test
    void unUsuarioDesactivadoNoPuedeSeguirUsandoSuTokenNiEntrar() throws Exception {
        String tokenLaura = iniciarSesion("investigador1@biohub.test", DatosDePrueba.CLAVE);

        mvc.perform(con(post("/interno/usuarios/{id}/desactivar", laura.getId()), tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mvc.perform(con(get("/interno/prueba/registrar"), tokenLaura)).andExpect(status().isUnauthorized());
        mvc.perform(post("/interno/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"investigador1@biohub.test\",\"contrasena\":\"biohub123\"}"))
                .andExpect(status().isUnauthorized());

        // No se borró: sigue en la lista y se puede volver a activar
        mvc.perform(con(post("/interno/usuarios/{id}/activar", laura.getId()), tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        mvc.perform(con(get("/interno/prueba/registrar"), tokenLaura)).andExpect(status().isOk());
    }

    @Test
    void elAdministradorNoPuedeDesactivarse() throws Exception {
        Usuario admin = almacen.buscarPorCorreo("admin@biohub.test").orElseThrow();

        mvc.perform(con(post("/interno/usuarios/{id}/desactivar", admin.getId()), tokenAdmin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No puedes desactivarte a ti mismo."));
    }

    // ---------- Otros ----------

    @Test
    void cambiarElRolDeUnUsuario() throws Exception {
        mvc.perform(con(put("/interno/usuarios/{id}/rol", andres.getId()), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rolId\":\"" + almacen.auditor.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nivel").value("AUDITOR"));
    }

    @Test
    void unCorreoRepetidoResponde409() throws Exception {
        mvc.perform(con(post("/interno/usuarios"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Laura 2\",\"correo\":\"investigador1@biohub.test\","
                                + "\"contrasena\":\"clave-segura\",\"rolId\":\"" + almacen.auditor.getId() + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void laListaDeUsuariosNoExponeContrasenas() throws Exception {
        String json = mvc.perform(con(get("/interno/usuarios"), tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse().getContentAsString();

        assertThat(json.toLowerCase()).doesNotContain("contrasena", "$2a$");
    }

    @Test
    void listaRolesYPermisos() throws Exception {
        mvc.perform(con(get("/interno/roles"), tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Administrador del sistema"));
        mvc.perform(con(get("/interno/permisos"), tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(AlmacenEnMemoria.CATALOGO.length));
    }
}
