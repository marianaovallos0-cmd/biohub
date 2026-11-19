package co.edu.unbosque.biohub.interna.taxonomia;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.DefinicionAtributo;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.RepositorioFichas;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaTaxonomica;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.RepositorioTaxonomia;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoDato;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Los dos tipos de especie del acta (igual que los datos de prueba de la base), sin base de datos.
public final class TaxonomiaDePrueba {

    private TaxonomiaDePrueba() { }

    // Rana: temperatura del agua (obligatoria), grabación acústica y longitud.
    public static TipoEspecie anfibio() {
        TipoEspecie anfibio = conId(new TipoEspecie("Anfibio"));
        conId(anfibio.agregarAtributo("temperatura_agua_c", TipoDato.NUMERO, true));
        conId(anfibio.agregarAtributo("grabacion_acustica", TipoDato.ARCHIVO, false));
        conId(anfibio.agregarAtributo("longitud_hocico_cloaca_mm", TipoDato.NUMERO, false));
        return anfibio;
    }

    // Ave: ruta GPS (obligatoria), muestra de ADN y fecha de anillado.
    public static TipoEspecie ave() {
        TipoEspecie ave = conId(new TipoEspecie("Ave"));
        conId(ave.agregarAtributo("ruta_gps", TipoDato.TEXTO, true));
        conId(ave.agregarAtributo("muestra_adn", TipoDato.TEXTO, false));
        conId(ave.agregarAtributo("fecha_anillado", TipoDato.FECHA, false));
        return ave;
    }

    public static UUID idDe(TipoEspecie tipo, String nombreAtributo) {
        return tipo.getDefiniciones().stream()
                .filter(d -> d.getNombre().equals(nombreAtributo))
                .map(DefinicionAtributo::getId)
                .findFirst().orElseThrow();
    }

    // En la base el id lo pone PostgreSQL; aquí lo ponemos a mano.
    private static <T> T conId(T entidad) {
        ReflectionTestUtils.setField(entidad, "id", UUID.randomUUID());
        return entidad;
    }

    // Doble tipo fake de RepositorioTaxonomia.
    public static class RepositorioTaxonomiaFalso implements RepositorioTaxonomia {

        public final List<TipoEspecie> tipos = new ArrayList<>();

        @Override
        public List<TipoEspecie> listarTipos() {
            return tipos;
        }

        @Override
        public Optional<TipoEspecie> buscarTipo(UUID id) {
            return tipos.stream().filter(t -> t.getId().equals(id)).findFirst();
        }
    }

    // Doble tipo fake de RepositorioFichas.
    public static class RepositorioFichasFalso implements RepositorioFichas {

        public final List<FichaTaxonomica> guardadas = new ArrayList<>();

        @Override
        public void guardar(FichaTaxonomica ficha) {
            guardadas.add(ficha);
        }

        @Override
        public Optional<FichaTaxonomica> buscarVigente(UUID hallazgoId) {
            return guardadas.stream().filter(f -> f.getHallazgoId().equals(hallazgoId))
                    .max(java.util.Comparator.comparingInt(FichaTaxonomica::getNumeroVersion));
        }
    }
}
