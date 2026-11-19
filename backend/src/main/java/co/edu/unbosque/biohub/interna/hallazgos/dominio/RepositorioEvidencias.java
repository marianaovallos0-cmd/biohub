package co.edu.unbosque.biohub.interna.hallazgos.dominio;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioEvidencias {

    void guardar(Evidencia evidencia);

    Optional<Evidencia> buscarPorId(UUID id);
}
