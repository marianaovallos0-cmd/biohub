package co.edu.unbosque.biohub.interna.historial;

import co.edu.unbosque.biohub.interna.historial.dominio.RegistroCambio;
import co.edu.unbosque.biohub.interna.historial.dominio.RepositorioCambios;
import co.edu.unbosque.biohub.interna.historial.dominio.VersionDuplicadaException;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion;
import co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaTaxonomica;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.taxonomia.TaxonomiaDePrueba.idDe;

// Dobles y datos para HU-06, HU-07 y HU-08, sin base de datos.
public final class TrazabilidadDePrueba {

    public static final OffsetDateTime REGISTRO = OffsetDateTime.of(2026, 8, 5, 9, 30, 0, 0, ZoneOffset.ofHours(-5));
    public static final String JUSTIFICACION =
            "La especie pasó del género Dendrobates a Oophaga según la revisión de la familia Dendrobatidae.";

    private TrazabilidadDePrueba() { }

    public static UsuarioAutenticado auditor() {
        return new UsuarioAutenticado(UUID.randomUUID(), "Carolina Vélez", "auditor@biohub.test",
                NivelAutorizacion.AUDITOR, Set.of("RECLASIFICAR_HALLAZGO", "VER_HISTORIAL"));
    }

    // Como el hallazgo 1 de los datos de prueba: registrado con el nombre antiguo y con una grabación
    public static FichaTaxonomica fichaOriginal(TipoEspecie anfibio, UUID hallazgoId) {
        return conId(FichaTaxonomica.builder(anfibio)
                .paraHallazgo(hallazgoId).version(1).fechaVersion(REGISTRO)
                .reino("Animalia").filo("Chordata").clase("Amphibia").orden("Anura")
                .familia("Dendrobatidae").genero("Dendrobates").especie("histrionicus")
                .atributo(idDe(anfibio, "temperatura_agua_c"), "24.5")
                .atributo(idDe(anfibio, "grabacion_acustica"), "evidencias/canto-001.wav")
                .build());
    }

    public static FichaTaxonomica conId(FichaTaxonomica ficha) {
        if (ficha.getId() == null) {
            ReflectionTestUtils.setField(ficha, "id", UUID.randomUUID());
        }
        return ficha;
    }

    // Igual que el de TaxonomiaDePrueba, pero pone el id como lo hace PostgreSQL al guardar
    public static class RepositorioFichasConId extends TaxonomiaDePrueba.RepositorioFichasFalso {
        @Override
        public void guardar(FichaTaxonomica ficha) {
            super.guardar(conId(ficha));
        }
    }

    public static class RepositorioCambiosFalso implements RepositorioCambios {

        public final List<RegistroCambio> guardados = new ArrayList<>();
        public final List<FichaTaxonomica> fichas;
        // Para simular que otro auditor guardó la misma versión un instante antes
        public boolean chocarAlGuardar;

        public RepositorioCambiosFalso(List<FichaTaxonomica> fichas) {
            this.fichas = fichas;
        }

        @Override
        public void guardar(RegistroCambio cambio) {
            if (chocarAlGuardar) {
                throw new VersionDuplicadaException();
            }
            ReflectionTestUtils.setField(cambio, "id", UUID.randomUUID());
            guardados.add(cambio);
        }

        @Override
        public List<RegistroCambio> listarPorHallazgo(UUID hallazgoId) {
            return guardados.stream()
                    .filter(c -> fichas.stream().anyMatch(f ->
                            f.getId().equals(c.getVersionNuevaId()) && f.getHallazgoId().equals(hallazgoId)))
                    .sorted(Comparator.comparing(RegistroCambio::getFechaHora))
                    .toList();
        }
    }
}
