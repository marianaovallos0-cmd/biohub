package co.edu.unbosque.biohub.publica.consultapublica.aplicacion;

import co.edu.unbosque.biohub.publica.consultapublica.dominio.BusquedaInvalidaException;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.RepositorioConsultaPublica;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.ResultadoBusqueda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuscarEspeciesYHallazgosTest {

    /** Doble del repositorio: guarda los datos en una lista y anota con qué lo llamaron. */
    static class RepositorioFalso implements RepositorioConsultaPublica {
        final List<ResultadoBusqueda> datos = new ArrayList<>();
        final List<String> textosRecibidos = new ArrayList<>();
        int limiteRecibido;

        @Override
        public List<ResultadoBusqueda> buscarPorNombreEspecie(String texto, int limite) {
            textosRecibidos.add(texto);
            limiteRecibido = limite;
            return datos.stream()
                    .filter(r -> r.nombreCientifico().toLowerCase().contains(texto))
                    .limit(limite)
                    .toList();
        }
    }

    private RepositorioFalso repositorio;
    private BuscarEspeciesYHallazgos buscar;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioFalso();
        repositorio.datos.add(resultado("Ara macao", "Amazonas"));
        repositorio.datos.add(resultado("Ara macao", "Amazonas"));
        repositorio.datos.add(resultado("Oophaga histrionica", "Chocó"));
        buscar = new BuscarEspeciesYHallazgos(repositorio);
    }

    @Test
    void buscarPorNombreDeEspecieDevuelveLasCoincidencias() {
        List<ResultadoBusqueda> resultados = buscar.ejecutar("macao");

        assertThat(resultados).hasSize(2)
                .allSatisfy(r -> assertThat(r.nombreCientifico()).isEqualTo("Ara macao"));
    }

    @Test
    void sinCoincidenciasDevuelveListaVacia() {
        assertThat(buscar.ejecutar("panthera")).isEmpty();
    }

    @Test
    void normalizaElTextoAntesDeBuscar() {
        buscar.ejecutar("   OOPHAGA    Histrionica ");

        assertThat(repositorio.textosRecibidos).containsExactly("oophaga histrionica");
    }

    @Test
    void pideComoMaximoElLimiteDeResultados() {
        buscar.ejecutar("ara");

        assertThat(repositorio.limiteRecibido).isEqualTo(BuscarEspeciesYHallazgos.LIMITE_RESULTADOS);
    }

    @Test
    void rechazaUnaBusquedaVaciaOMuyCorta() {
        assertThatThrownBy(() -> buscar.ejecutar("  ")).isInstanceOf(BusquedaInvalidaException.class);
        assertThatThrownBy(() -> buscar.ejecutar("a")).isInstanceOf(BusquedaInvalidaException.class);
        assertThatThrownBy(() -> buscar.ejecutar(null)).isInstanceOf(BusquedaInvalidaException.class);
        assertThat(repositorio.textosRecibidos).isEmpty();
    }

    @Test
    void rechazaUnaBusquedaDemasiadoLarga() {
        assertThatThrownBy(() -> buscar.ejecutar("a".repeat(101)))
                .isInstanceOf(BusquedaInvalidaException.class);
    }

    private static ResultadoBusqueda resultado(String nombre, String departamento) {
        return new ResultadoBusqueda(UUID.randomUUID(), nombre, "Ave", "Aves", "Psittaciformes",
                "Psittacidae", "Amazonía", departamento, OffsetDateTime.now());
    }
}
