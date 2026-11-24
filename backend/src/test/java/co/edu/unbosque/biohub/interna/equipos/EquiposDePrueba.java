package co.edu.unbosque.biohub.interna.equipos;

import co.edu.unbosque.biohub.interna.equipos.dominio.DirectorioInvestigadores;
import co.edu.unbosque.biohub.interna.equipos.dominio.EquipoCampo;
import co.edu.unbosque.biohub.interna.equipos.dominio.RepositorioEquipos;
import co.edu.unbosque.biohub.interna.equipos.dominio.Responsable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Dobles tipo fake de HU-04: guardan en listas en vez de PostgreSQL
public final class EquiposDePrueba {

    // Los dos investigadores de los datos de prueba
    public static final Responsable LAURA = new Responsable(UUID.randomUUID(), "Laura Gómez", "investigador1@biohub.test");
    public static final Responsable ANDRES = new Responsable(UUID.randomUUID(), "Andrés Rojas", "investigador2@biohub.test");

    private EquiposDePrueba() { }

    public static class RepositorioEquiposFalso implements RepositorioEquipos {

        public final List<EquipoCampo> guardados = new ArrayList<>();

        @Override
        public void guardar(EquipoCampo equipo) {
            // En la base el id lo pone PostgreSQL; aquí lo ponemos a mano
            ReflectionTestUtils.setField(equipo, "id", UUID.randomUUID());
            guardados.add(equipo);
        }

        @Override
        public boolean existeCodigo(String codigoIdentificacion) {
            return guardados.stream().anyMatch(e -> e.getCodigoIdentificacion().equals(codigoIdentificacion));
        }

        @Override
        public List<EquipoCampo> listar() {
            return guardados.stream().sorted(Comparator.comparing(EquipoCampo::getCodigoIdentificacion)).toList();
        }

        @Override
        public List<EquipoCampo> listarPorInvestigador(UUID investigadorId) {
            return listar().stream().filter(e -> e.getInvestigadorId().equals(investigadorId)).toList();
        }
    }

    public static class DirectorioFalso implements DirectorioInvestigadores {

        public final List<Responsable> activos = new ArrayList<>(List.of(LAURA, ANDRES));

        @Override
        public Optional<Responsable> buscarActivo(UUID investigadorId) {
            return activos.stream().filter(r -> r.id().equals(investigadorId)).findFirst();
        }

        @Override
        public List<Responsable> listarActivos() {
            return activos;
        }
    }
}
