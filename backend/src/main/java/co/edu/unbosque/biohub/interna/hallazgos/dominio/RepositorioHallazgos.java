package co.edu.unbosque.biohub.interna.hallazgos.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioHallazgos {

    void guardar(Hallazgo hallazgo);

    Optional<Hallazgo> buscarPorId(UUID id);

    // Del más reciente al más antiguo
    List<Hallazgo> listarPorInvestigador(UUID investigadorId);
}
