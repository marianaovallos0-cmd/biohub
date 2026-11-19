package co.edu.unbosque.biohub.interna.hallazgos.dominio;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioHallazgos {

    void guardar(Hallazgo hallazgo);

    Optional<Hallazgo> buscarPorId(UUID id);
}
