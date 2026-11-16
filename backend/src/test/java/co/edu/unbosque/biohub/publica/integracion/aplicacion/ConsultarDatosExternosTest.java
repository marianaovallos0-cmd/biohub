package co.edu.unbosque.biohub.publica.integracion.aplicacion;

import co.edu.unbosque.biohub.publica.integracion.dominio.HallazgoExterno;
import co.edu.unbosque.biohub.publica.integracion.dominio.HallazgoNoEncontradoException;
import co.edu.unbosque.biohub.publica.integracion.dominio.PaginaInvalidaException;
import co.edu.unbosque.biohub.publica.integracion.dominio.RepositorioDatosExternos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarDatosExternosTest {

    /** Doble del repositorio: una lista en memoria que se recorta como lo haría LIMIT/OFFSET. */
    static class RepositorioFalso implements RepositorioDatosExternos {
        final List<HallazgoExterno> datos = new ArrayList<>();

        @Override
        public List<HallazgoExterno> listar(int desde, int cantidad) {
            return datos.stream().skip(desde).limit(cantidad).toList();
        }

        @Override
        public Optional<HallazgoExterno> buscarPorId(UUID hallazgoId) {
            return datos.stream().filter(h -> h.hallazgoId().equals(hallazgoId)).findFirst();
        }
    }

    private RepositorioFalso repositorio;
    private ConsultarDatosExternos consultar;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioFalso();
        for (int i = 0; i < 5; i++) {
            repositorio.datos.add(hallazgo(UUID.randomUUID(), "Ara macao"));
        }
        consultar = new ConsultarDatosExternos(repositorio);
    }

    @Test
    void devuelveLaPaginaPedida() {
        assertThat(consultar.listar(0, 2)).containsExactly(repositorio.datos.get(0), repositorio.datos.get(1));
        assertThat(consultar.listar(1, 2)).containsExactly(repositorio.datos.get(2), repositorio.datos.get(3));
        assertThat(consultar.listar(2, 2)).containsExactly(repositorio.datos.get(4));
    }

    @Test
    void unaPaginaDespuesDelFinalVieneVacia() {
        assertThat(consultar.listar(10, 2)).isEmpty();
    }

    @Test
    void rechazaPaginasYTamanosFueraDeRango() {
        assertThatThrownBy(() -> consultar.listar(-1, 10)).isInstanceOf(PaginaInvalidaException.class);
        assertThatThrownBy(() -> consultar.listar(0, 0)).isInstanceOf(PaginaInvalidaException.class);
        assertThatThrownBy(() -> consultar.listar(0, ConsultarDatosExternos.TAMANO_MAXIMO + 1))
                .isInstanceOf(PaginaInvalidaException.class);
    }

    @Test
    void buscaUnHallazgoPorId() {
        HallazgoExterno esperado = repositorio.datos.get(3);

        assertThat(consultar.buscar(esperado.hallazgoId())).isEqualTo(esperado);
    }

    @Test
    void unHallazgoQueNoExisteLanzaNoEncontrado() {
        assertThatThrownBy(() -> consultar.buscar(UUID.randomUUID()))
                .isInstanceOf(HallazgoNoEncontradoException.class);
    }

    @Test
    void elHallazgoExternoNoTieneCoordenadas() {
        // Si alguien le agrega latitud o longitud al record, esta prueba falla (ASR-1)
        assertThat(HallazgoExterno.class.getRecordComponents())
                .extracting(c -> c.getName().toLowerCase())
                .noneMatch(nombre -> nombre.contains("latitud") || nombre.contains("longitud"));
    }

    static HallazgoExterno hallazgo(UUID id, String nombre) {
        String[] partes = nombre.split(" ");
        return new HallazgoExterno(id, nombre, "Ave", "Animalia", "Chordata", "Aves", "Psittaciformes",
                "Psittacidae", partes[0], partes[1], "Amazonía", "Amazonas",
                OffsetDateTime.parse("2026-08-15T06:30-05:00"));
    }
}
