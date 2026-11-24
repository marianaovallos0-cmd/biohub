package co.edu.unbosque.biohub.publica.consultapublica.web;

import co.edu.unbosque.biohub.publica.consultapublica.dominio.RepositorioConsultaPublica;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.ResultadoBusqueda;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración del endpoint con el perfil publica: seguridad, controlador,
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
class BusquedaPublicaControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RepositorioConsultaPublica repositorio;

    @Test
    void buscarNoPideLoginYDevuelveLasCoincidencias() throws Exception {
        when(repositorio.buscarPorNombreEspecie(eq("vultur"), anyInt())).thenReturn(List.of(
                new ResultadoBusqueda(UUID.randomUUID(), "Vultur gryphus", "Ave", "Aves",
                        "Cathartiformes", "Cathartidae", "Andina", "Boyacá", OffsetDateTime.now())));

        mvc.perform(get("/publico/busqueda").param("q", "Vultur"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consulta").value("Vultur"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.resultados[0].nombreCientifico").value("Vultur gryphus"))
                .andExpect(jsonPath("$.resultados[0].departamento").value("Boyacá"))
                .andExpect(jsonPath("$.resultados[0].latitud").doesNotExist())
                .andExpect(jsonPath("$.mensaje").doesNotExist());
    }

    @Test
    void sinCoincidenciasInformaQueNoHayResultados() throws Exception {
        when(repositorio.buscarPorNombreEspecie(eq("panthera onca"), anyInt())).thenReturn(List.of());

        mvc.perform(get("/publico/busqueda").param("q", "Panthera onca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.resultados").isEmpty())
                .andExpect(jsonPath("$.mensaje").value("No hay resultados para \"Panthera onca\"."));
    }

    @Test
    void unaBusquedaVaciaDevuelve400() throws Exception {
        mvc.perform(get("/publico/busqueda").param("q", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());

        verify(repositorio, never()).buscarPorNombreEspecie(anyString(), anyInt());
    }

    @Test
    void laMismaBusquedaRepetidaSaleDeLaCache() throws Exception {
        when(repositorio.buscarPorNombreEspecie(eq("rupicola"), anyInt())).thenReturn(List.of());

        mvc.perform(get("/publico/busqueda").param("q", "Rupicola")).andExpect(status().isOk());
        mvc.perform(get("/publico/busqueda").param("q", "  rupicola ")).andExpect(status().isOk());

        verify(repositorio, times(1)).buscarPorNombreEspecie(eq("rupicola"), anyInt());
    }

    @Test
    void laApiPublicaNoAceptaEscrituras() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/publico/busqueda").param("q", "ara"))
                .andExpect(status().isForbidden());
    }
}
