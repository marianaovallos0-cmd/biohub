package co.edu.unbosque.biohub.publica.consultapublica.web;

import co.edu.unbosque.biohub.publica.consultapublica.dominio.FichaPublica;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.RepositorioFichaPublica;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.UbicacionPublica;
import co.edu.unbosque.biohub.publica.consultapublica.aplicacion.VerFichaPublica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración de la HU-10 con el perfil publica: seguridad, controlador,
 * caso de uso y caché juntos. Solo el repositorio es un doble, para no necesitar
 * PostgreSQL ni Redis al correr las pruebas.
 */
@SpringBootTest(properties = {
        "spring.cache.type=simple",
        "management.health.redis.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("publica")
class FichaPublicaControllerTest {

    private static final UUID CONDOR = UUID.fromString("40000000-0000-0000-0000-000000000009");
    private static final UUID GALLITO_DE_ROCA = UUID.fromString("40000000-0000-0000-0000-000000000011");

    @Autowired
    MockMvc mvc;

    @Autowired
    CacheManager caches;

    @MockitoBean
    RepositorioFichaPublica repositorio;

    // El contexto se comparte entre pruebas: se limpia la caché para que cada una arranque igual
    @BeforeEach
    void limpiarCache() {
        caches.getCache(VerFichaPublica.CACHE).clear();
    }

    @Test
    void unVisitanteSinLoginVeLaFichaConRegionYDepartamento() throws Exception {
        when(repositorio.buscarPorHallazgo(CONDOR)).thenReturn(Optional.of(ficha(CONDOR,
                new UbicacionPublica("Andina", "Boyacá"))));

        mvc.perform(get("/publico/fichas/{id}", CONDOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hallazgoId").value(CONDOR.toString()))
                .andExpect(jsonPath("$.nombreCientifico").value("Vultur gryphus"))
                .andExpect(jsonPath("$.familia").value("Cathartidae"))
                .andExpect(jsonPath("$.ubicacion.region").value("Andina"))
                .andExpect(jsonPath("$.ubicacion.departamento").value("Boyacá"));
    }

    @Test
    void laRespuestaNoTraeCoordenadasPorNingunLado() throws Exception {
        when(repositorio.buscarPorHallazgo(CONDOR)).thenReturn(Optional.of(ficha(CONDOR,
                new UbicacionPublica("Andina", "Boyacá"))));

        String json = mvc.perform(get("/publico/fichas/{id}", CONDOR))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Tercer criterio de la HU-10: ni en la pantalla ni en la respuesta del servidor
        assertThat(json.toLowerCase()).doesNotContain("latitud", "longitud", "coordenada");
    }

    @Test
    void unHallazgoSinUbicacionDevuelveUbicacionNula() throws Exception {
        when(repositorio.buscarPorHallazgo(GALLITO_DE_ROCA)).thenReturn(Optional.of(ficha(GALLITO_DE_ROCA, null)));

        mvc.perform(get("/publico/fichas/{id}", GALLITO_DE_ROCA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ubicacion").isEmpty());
    }

    @Test
    void unaFichaQueNoExisteDevuelve404() throws Exception {
        UUID inexistente = UUID.randomUUID();
        when(repositorio.buscarPorHallazgo(inexistente)).thenReturn(Optional.empty());

        mvc.perform(get("/publico/fichas/{id}", inexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void unIdQueNoEsUuidDevuelve400SinConsultarLaBase() throws Exception {
        mvc.perform(get("/publico/fichas/no-es-un-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El id de la ficha no es válido."));

        verify(repositorio, never()).buscarPorHallazgo(any());
    }

    @Test
    void laMismaFichaRepetidaSaleDeLaCache() throws Exception {
        when(repositorio.buscarPorHallazgo(CONDOR)).thenReturn(Optional.of(ficha(CONDOR,
                new UbicacionPublica("Andina", "Boyacá"))));

        mvc.perform(get("/publico/fichas/{id}", CONDOR)).andExpect(status().isOk());
        mvc.perform(get("/publico/fichas/{id}", CONDOR)).andExpect(status().isOk());

        verify(repositorio, times(1)).buscarPorHallazgo(CONDOR);
    }

    @Test
    void laFichaNoAceptaEscrituras() throws Exception {
        mvc.perform(post("/publico/fichas/{id}", CONDOR)).andExpect(status().isForbidden());
    }

    private static FichaPublica ficha(UUID id, UbicacionPublica ubicacion) {
        return new FichaPublica(id, "Vultur gryphus", "Ave", "Animalia", "Chordata", "Aves",
                "Cathartiformes", "Cathartidae", "Vultur", "gryphus",
                OffsetDateTime.parse("2026-09-22T11:00-05:00"), ubicacion);
    }
}
