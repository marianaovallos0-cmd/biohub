package co.edu.unbosque.biohub.interna.equipos.web;

import co.edu.unbosque.biohub.interna.equipos.EquiposDePrueba;
import co.edu.unbosque.biohub.interna.equipos.dominio.DirectorioInvestigadores;
import co.edu.unbosque.biohub.interna.equipos.dominio.EquipoCampo;
import co.edu.unbosque.biohub.interna.equipos.dominio.RepositorioEquipos;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.equipos.EquiposDePrueba.LAURA;
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

// HU-04 de punta a punta con el perfil interna. Los repositorios son dobles, no hace falta PostgreSQL.
@SpringBootTest(properties = {
        "biohub.jwt.secreto=clave-solo-para-las-pruebas-de-biohub-2026",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("interna")
class EquiposControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RepositorioUsuarios repositorioUsuarios;

    @MockitoBean
    RepositorioIntentos repositorioIntentos;

    @MockitoBean
    RepositorioEquipos repositorioEquipos;

    @MockitoBean
    DirectorioInvestigadores directorio;

    @MockitoBean
    PlatformTransactionManager transacciones;

    private Usuario laura;

    @BeforeEach
    void preparar() {
        Usuario admin = usuario("Admin BioHub", "admin@biohub.test", rol(ADMINISTRADOR, "GESTIONAR_EQUIPOS", "GESTIONAR_USUARIOS"));
        laura = usuario("Laura Gómez", "investigador1@biohub.test", rol(INVESTIGADOR, "REGISTRAR_HALLAZGO"));
        List<Usuario> usuarios = List.of(admin, laura);

        when(repositorioUsuarios.buscarPorCorreo(anyString())).thenAnswer(i -> usuarios.stream()
                .filter(u -> u.getCorreo().equals(i.getArgument(0))).findFirst());
        when(repositorioUsuarios.buscarPorId(any(UUID.class))).thenAnswer(i -> usuarios.stream()
                .filter(u -> u.getId().equals(i.getArgument(0))).findFirst());
        when(directorio.buscarActivo(LAURA.id())).thenReturn(Optional.of(LAURA));
        doAnswer(i -> {
            Object equipo = i.getArgument(0);
            ReflectionTestUtils.setField(equipo, "id", UUID.randomUUID());
            return null;
        }).when(repositorioEquipos).guardar(any());
    }

    private String iniciarSesion(String correo) throws Exception {
        String json = mvc.perform(post("/interno/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private static String equipo(String codigo, String investigadorId) {
        String responsable = investigadorId == null ? "null" : "\"" + investigadorId + "\"";
        return "{\"codigoIdentificacion\":\"" + codigo + "\",\"tipo\":\"DRON\",\"investigadorId\":" + responsable + "}";
    }

    @Test
    void elAdministradorRegistraUnEquipoConSuResponsable() throws Exception {
        String token = iniciarSesion("admin@biohub.test");

        mvc.perform(post("/interno/equipos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(equipo("drn-077", LAURA.id().toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoIdentificacion").value("DRN-077"))
                .andExpect(jsonPath("$.tipo").value("DRON"))
                .andExpect(jsonPath("$.responsable").value("Laura Gómez"));

        ArgumentCaptor<EquipoCampo> guardado = ArgumentCaptor.forClass(EquipoCampo.class);
        verify(repositorioEquipos).guardar(guardado.capture());
        assertThat(guardado.getValue().getInvestigadorId()).isEqualTo(LAURA.id());
    }

    @Test
    void sinResponsableResponde400YNoGuarda() throws Exception {
        String token = iniciarSesion("admin@biohub.test");

        mvc.perform(post("/interno/equipos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(equipo("DRN-077", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("Falta el investigador responsable."));
        verify(repositorioEquipos, never()).guardar(any());
    }

    @Test
    void unCodigoRepetidoResponde409() throws Exception {
        when(repositorioEquipos.existeCodigo("DRN-001")).thenReturn(true);
        String token = iniciarSesion("admin@biohub.test");

        mvc.perform(post("/interno/equipos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(equipo("DRN-001", LAURA.id().toString())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un equipo con la identificación «DRN-001»."));
        verify(repositorioEquipos, never()).guardar(any());
    }

    // Solo el rol administrador registra equipos
    @Test
    void unInvestigadorNoRegistraEquiposYSuIntentoQuedaGuardado() throws Exception {
        String token = iniciarSesion("investigador1@biohub.test");

        mvc.perform(post("/interno/equipos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(equipo("DRN-077", LAURA.id().toString())))
                .andExpect(status().isForbidden());

        ArgumentCaptor<IntentoAccesoNoAutorizado> intento = ArgumentCaptor.forClass(IntentoAccesoNoAutorizado.class);
        verify(repositorioIntentos).guardar(intento.capture());
        assertThat(intento.getValue().getUsuario()).isSameAs(laura);
        assertThat(intento.getValue().getRecursoSolicitado()).isEqualTo("POST /interno/equipos");
        verify(repositorioEquipos, never()).guardar(any());
    }

    @Test
    void elAdministradorVeLosResponsablesPosibles() throws Exception {
        when(directorio.listarActivos()).thenReturn(List.of(EquiposDePrueba.LAURA, EquiposDePrueba.ANDRES));
        String token = iniciarSesion("admin@biohub.test");

        mvc.perform(get("/interno/equipos/responsables").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Laura Gómez"))
                .andExpect(jsonPath("$[1].nombre").value("Andrés Rojas"));
    }
}
