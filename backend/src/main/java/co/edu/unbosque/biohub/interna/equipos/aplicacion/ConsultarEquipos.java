package co.edu.unbosque.biohub.interna.equipos.aplicacion;

import co.edu.unbosque.biohub.interna.equipos.dominio.DirectorioInvestigadores;
import co.edu.unbosque.biohub.interna.equipos.dominio.RepositorioEquipos;
import co.edu.unbosque.biohub.interna.equipos.dominio.Responsable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// HU-04: lo que necesita la pantalla del administrador (los equipos ya registrados y a quién se pueden asignar)
@Service
public class ConsultarEquipos {

    private final RepositorioEquipos equipos;
    private final DirectorioInvestigadores investigadores;

    public ConsultarEquipos(RepositorioEquipos equipos, DirectorioInvestigadores investigadores) {
        this.equipos = equipos;
        this.investigadores = investigadores;
    }

    @Transactional(readOnly = true)
    public List<EquipoRegistrado> todos() {
        Map<UUID, String> nombres = investigadores.listarActivos().stream()
                .collect(Collectors.toMap(Responsable::id, Responsable::nombre));
        return equipos.listar().stream()
                .map(e -> EquipoRegistrado.de(e, nombres.getOrDefault(e.getInvestigadorId(), "Investigador inactivo")))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Responsable> responsables() {
        return investigadores.listarActivos();
    }

}
