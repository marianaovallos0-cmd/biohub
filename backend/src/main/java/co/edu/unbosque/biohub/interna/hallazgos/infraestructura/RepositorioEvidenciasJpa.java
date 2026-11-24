package co.edu.unbosque.biohub.interna.hallazgos.infraestructura;

import co.edu.unbosque.biohub.interna.hallazgos.dominio.Evidencia;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioEvidencias;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioEvidenciasJpa implements RepositorioEvidencias {

    private final EvidenciaSpringData jpa;

    public RepositorioEvidenciasJpa(EvidenciaSpringData jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(Evidencia evidencia) {
        jpa.save(evidencia);
    }

    @Override
    public Optional<Evidencia> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<Evidencia> listarPorHallazgo(UUID hallazgoId) {
        return jpa.findByHallazgoIdOrderByFechaCargaAsc(hallazgoId);
    }
}
