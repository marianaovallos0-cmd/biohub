package co.edu.unbosque.biohub.publica.consultapublica.aplicacion;

import co.edu.unbosque.biohub.publica.consultapublica.dominio.FichaNoEncontradaException;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.FichaPublica;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.RepositorioFichaPublica;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.UbicacionPublica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerFichaPublicaTest {

    /** Doble del repositorio: guarda las fichas en un mapa en vez de PostgreSQL. */
    static class RepositorioFalso implements RepositorioFichaPublica {
        final Map<UUID, FichaPublica> fichas = new HashMap<>();

        @Override
        public Optional<FichaPublica> buscarPorHallazgo(UUID hallazgoId) {
            return Optional.ofNullable(fichas.get(hallazgoId));
        }
    }

    private static final UUID GUACAMAYA = UUID.fromString("40000000-0000-0000-0000-000000000005");
    private static final UUID GALLITO_DE_ROCA = UUID.fromString("40000000-0000-0000-0000-000000000011");

    private RepositorioFalso repositorio;
    private VerFichaPublica verFicha;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioFalso();
        repositorio.fichas.put(GUACAMAYA, ficha(GUACAMAYA, "Ara", "macao",
                new UbicacionPublica("Amazonía", "Amazonas")));
        repositorio.fichas.put(GALLITO_DE_ROCA, ficha(GALLITO_DE_ROCA, "Rupicola", "peruvianus", null));
        verFicha = new VerFichaPublica(repositorio);
    }

    @Test
    void devuelveLaFichaConSuRegionYDepartamento() {
        FichaPublica ficha = verFicha.ejecutar(GUACAMAYA);

        assertThat(ficha.nombreCientifico()).isEqualTo("Ara macao");
        assertThat(ficha.ubicacion().region()).isEqualTo("Amazonía");
        assertThat(ficha.ubicacion().departamento()).isEqualTo("Amazonas");
    }

    @Test
    void unHallazgoSinUbicacionDevuelveLaFichaSinUbicacion() {
        FichaPublica ficha = verFicha.ejecutar(GALLITO_DE_ROCA);

        assertThat(ficha.nombreCientifico()).isEqualTo("Rupicola peruvianus");
        assertThat(ficha.ubicacion()).isNull();
    }

    @Test
    void unHallazgoQueNoExisteLanzaFichaNoEncontrada() {
        UUID inexistente = UUID.randomUUID();

        assertThatThrownBy(() -> verFicha.ejecutar(inexistente))
                .isInstanceOf(FichaNoEncontradaException.class)
                .hasMessageContaining(inexistente.toString());
    }

    @Test
    void laUbicacionPublicaNoTieneCoordenadas() {
        // Si alguien le agrega latitud o longitud al record, esta prueba falla (ASR-1)
        assertThat(UbicacionPublica.class.getRecordComponents())
                .extracting(c -> c.getName())
                .containsExactly("region", "departamento");
    }

    static FichaPublica ficha(UUID id, String genero, String especie, UbicacionPublica ubicacion) {
        return new FichaPublica(id, genero + " " + especie, "Ave", "Animalia", "Chordata", "Aves",
                "Psittaciformes", "Psittacidae", genero, especie,
                OffsetDateTime.parse("2026-08-15T06:30-05:00"), ubicacion);
    }
}
