package co.edu.unbosque.biohub.interna.equipos.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Lo que Equipos necesita saber de los investigadores: solo un investigador activo puede ser responsable
public interface DirectorioInvestigadores {

    Optional<Responsable> buscarActivo(UUID investigadorId);

    List<Responsable> listarActivos();
}
