package co.edu.unbosque.biohub.interna.hallazgos.infraestructura;

import co.edu.unbosque.biohub.interna.hallazgos.dominio.Hallazgo;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioHallazgos;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class RepositorioHallazgosJpa implements RepositorioHallazgos {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void guardar(Hallazgo hallazgo) {
        em.persist(hallazgo);
    }

    @Override
    public Optional<Hallazgo> buscarPorId(UUID id) {
        return Optional.ofNullable(em.find(Hallazgo.class, id));
    }
}
