package co.edu.unbosque.biohub.publica.integracion.web;

import co.edu.unbosque.biohub.publica.consultapublica.dominio.RepositorioConsultaPublica;
import co.edu.unbosque.biohub.publica.integracion.dominio.ConsumidorExterno;
import co.edu.unbosque.biohub.publica.integracion.dominio.HallazgoExterno;
import co.edu.unbosque.biohub.publica.integracion.dominio.RepositorioConsumidores;
import co.edu.unbosque.biohub.publica.integracion.dominio.RepositorioDatosExternos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración de la HU-16 con el perfil publica: filtro de la clave,
 * reglas de seguridad, controlador, caso de uso y caché juntos. Los repositorios
 * son dobles, para no necesitar PostgreSQL ni Redis.
 */
@SpringBootTest(properties = {
        "spring.cache.type=simple",
        "management.health.redis.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("publica")
class DatosExternosControllerTest {

    private static final String UNIVERSIDAD = "dev-key-universidad";
    private static final String ONG = "dev-key-ong";
    private static final UUID CONDOR = UUID.fromString("40000000-0000-0000-0000-000000000009");

    @Autowired
    MockMvc mvc;

    @Autowired
    CacheManager caches;

    @MockitoBean
    RepositorioConsumidores consumidores;

    @MockitoBean
    RepositorioDatosExternos datos;

    @MockitoBean
    RepositorioConsultaPublica consultaPublica;

    @BeforeEach
    void preparar() {
        caches.getCacheNames().forEach(nombre -> caches.getCache(nombre).clear());

        when(consumidores.buscarPorApiKey(anyString())).thenReturn(Optional.empty());
        when(consumidores.buscarPorApiKey(UNIVERSIDAD)).thenReturn(Optional.of(
                new ConsumidorExterno(UUID.randomUUID(), "Universidad de prueba", "UNIVERSIDAD", 120)));
        when(consumidores.buscarPorApiKey(ONG)).thenReturn(Optional.of(
                new ConsumidorExterno(UUID.randomUUID(), "ONG de prueba", "ONG", 60)));

        when(datos.listar(anyInt(), anyInt())).thenReturn(List.of(condor()));
        when(datos.buscarPorId(CONDOR)).thenReturn(Optional.of(condor()));
        when(consultaPublica.buscarPorNombreEspecie(anyString(), anyInt())).thenReturn(List.of());
    }

    @Test
    void laUniversidadConSuClaveObtieneLosHallazgos() throws Exception {
        mvc.perform(get("/externo/hallazgos").header("X-API-Key", UNIVERSIDAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(100))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.hallazgos[0].nombreCientifico").value("Vultur gryphus"))
                .andExpect(jsonPath("$.hallazgos[0].departamento").value("Boyacá"));

        verify(datos).listar(0, 100);
    }

    @Test
    void laOngConSuClaveObtieneUnHallazgo() throws Exception {
        mvc.perform(get("/externo/hallazgos/{id}", CONDOR).header("X-API-Key", ONG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hallazgoId").value(CONDOR.toString()))
                .andExpect(jsonPath("$.region").value("Andina"));
    }

    @Test
    void sePuedePedirOtraPaginaYOtroTamano() throws Exception {
        mvc.perform(get("/externo/hallazgos").param("pagina", "2").param("tamano", "50")
                        .header("X-API-Key", UNIVERSIDAD))
                .andExpect(status().isOk());

        verify(datos).listar(100, 50);
    }

    @Test
    void losDatosNoTraenCoordenadas() throws Exception {
        String lista = mvc.perform(get("/externo/hallazgos").header("X-API-Key", UNIVERSIDAD))
                .andReturn().getResponse().getContentAsString();
        String uno = mvc.perform(get("/externo/hallazgos/{id}", CONDOR).header("X-API-Key", UNIVERSIDAD))
                .andReturn().getResponse().getContentAsString();

        assertThat(lista.toLowerCase()).doesNotContain("latitud", "longitud");
        assertThat(uno.toLowerCase()).doesNotContain("latitud", "longitud");
    }


    @Test
    void sinClaveResponde401YNoTocaLosDatos() throws Exception {
        mvc.perform(get("/externo/hallazgos"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.detail").value("Falta el encabezado X-API-Key."));

        verifyNoInteractions(datos);
        verify(consumidores, never()).buscarPorApiKey(anyString());
    }

    @Test
    void conUnaClaveQueNoExisteResponde401() throws Exception {
        mvc.perform(get("/externo/hallazgos").header("X-API-Key", "clave-inventada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("La clave X-API-Key no es válida."));

        verifyNoInteractions(datos);
    }

    @Test
    void sinClaveTampocoSeAdivinanOtrasRutas() throws Exception {
        mvc.perform(get("/externo/coordenadas")).andExpect(status().isUnauthorized());
    }


    @Test
    void conClaveValidaUnaRutaNoHabilitadaResponde403() throws Exception {
        mvc.perform(get("/externo/coordenadas").header("X-API-Key", UNIVERSIDAD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Esta ruta no está habilitada."));
        mvc.perform(get("/externo/hallazgos/{id}/evidencias", CONDOR).header("X-API-Key", UNIVERSIDAD))
                .andExpect(status().isForbidden());
    }

    @Test
    void losExternosNoPuedenEscribir() throws Exception {
        mvc.perform(post("/externo/hallazgos").header("X-API-Key", UNIVERSIDAD))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/externo/hallazgos/{id}", CONDOR).header("X-API-Key", UNIVERSIDAD))
                .andExpect(status().isForbidden());

        verifyNoInteractions(datos);
    }


    @Test
    void unHallazgoQueNoExisteResponde404() throws Exception {
        UUID inexistente = UUID.randomUUID();
        when(datos.buscarPorId(inexistente)).thenReturn(Optional.empty());

        mvc.perform(get("/externo/hallazgos/{id}", inexistente).header("X-API-Key", UNIVERSIDAD))
                .andExpect(status().isNotFound());
    }

    @Test
    void unTamanoFueraDeRangoResponde400() throws Exception {
        mvc.perform(get("/externo/hallazgos").param("tamano", "5000").header("X-API-Key", UNIVERSIDAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El tamaño debe estar entre 1 y 500."));
    }

    @Test
    void laConsultaPublicaSigueSinPedirClave() throws Exception {
        mvc.perform(get("/publico/busqueda").param("q", "vultur"))
                .andExpect(status().isOk());
    }

    private static HallazgoExterno condor() {
        return new HallazgoExterno(CONDOR, "Vultur gryphus", "Ave", "Animalia", "Chordata", "Aves",
                "Cathartiformes", "Cathartidae", "Vultur", "gryphus", "Andina", "Boyacá",
                OffsetDateTime.parse("2026-09-22T11:00-05:00"));
    }
}