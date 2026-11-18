package co.edu.unbosque.biohub.publica.integracion.aplicacion;

import co.edu.unbosque.biohub.publica.integracion.dominio.ClaveInvalidaException;
import co.edu.unbosque.biohub.publica.integracion.dominio.ConsumidorExterno;
import co.edu.unbosque.biohub.publica.integracion.dominio.RepositorioConsumidores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AutenticarConsumidorTest {

    /** Doble del repositorio: las claves viven en un mapa en vez de PostgreSQL. */
    static class RepositorioFalso implements RepositorioConsumidores {
        final Map<String, ConsumidorExterno> porClave = new HashMap<>();
        int consultas;

        @Override
        public Optional<ConsumidorExterno> buscarPorApiKey(String apiKey) {
            consultas++;
            return Optional.ofNullable(porClave.get(apiKey));
        }
    }

    private RepositorioFalso repositorio;
    private AutenticarConsumidor autenticar;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioFalso();
        repositorio.porClave.put("dev-key-universidad",
                new ConsumidorExterno(UUID.randomUUID(), "Universidad de prueba", "UNIVERSIDAD", 120));
        repositorio.porClave.put("dev-key-ong",
                new ConsumidorExterno(UUID.randomUUID(), "ONG de prueba", "ONG", 60));
        autenticar = new AutenticarConsumidor(repositorio);
    }

    @Test
    void unaClaveValidaDevuelveSuConsumidor() {
        ConsumidorExterno consumidor = autenticar.ejecutar("dev-key-universidad");

        assertThat(consumidor.organizacion()).isEqualTo("Universidad de prueba");
        assertThat(consumidor.limitePorMinuto()).isEqualTo(120);
    }

    @Test
    void cadaClaveIdentificaASuPropioConsumidor() {
        assertThat(autenticar.ejecutar("dev-key-ong").tipo()).isEqualTo("ONG");
    }

    @Test
    void losEspaciosAlrededorDeLaClaveNoImportan() {
        assertThat(autenticar.ejecutar("  dev-key-ong ").organizacion()).isEqualTo("ONG de prueba");
    }

    @Test
    void sinClaveSeRechazaSinConsultarLaBase() {
        assertThatThrownBy(() -> autenticar.ejecutar(null))
                .isInstanceOf(ClaveInvalidaException.class)
                .hasMessage("Falta el encabezado X-API-Key.");
        assertThatThrownBy(() -> autenticar.ejecutar("   "))
                .isInstanceOf(ClaveInvalidaException.class);

        assertThat(repositorio.consultas).isZero();
    }

    @Test
    void unaClaveQueNoExisteSeRechaza() {
        assertThatThrownBy(() -> autenticar.ejecutar("clave-inventada"))
                .isInstanceOf(ClaveInvalidaException.class)
                .hasMessage("La clave X-API-Key no es válida.");
    }
}
