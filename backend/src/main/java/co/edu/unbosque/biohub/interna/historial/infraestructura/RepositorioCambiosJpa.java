package co.edu.unbosque.biohub.interna.historial.infraestructura;

import co.edu.unbosque.biohub.interna.historial.dominio.RegistroCambio;
import co.edu.unbosque.biohub.interna.historial.dominio.RepositorioCambios;
import co.edu.unbosque.biohub.interna.historial.dominio.VersionDuplicadaException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class RepositorioCambiosJpa implements RepositorioCambios {

    private final CambioSpringData jpa;

    public RepositorioCambiosJpa(CambioSpringData jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(RegistroCambio cambio) {
        try {
            // El flush escribe también la ficha nueva que está en la misma transacción.
            // Así el choque de (hallazgo_id, numero_version) sale aquí y no al hacer commit.
            jpa.saveAndFlush(cambio);
        } catch (DataIntegrityViolationException e) {
            throw new VersionDuplicadaException();
        }
    }

    @Override
    public List<RegistroCambio> listarPorHallazgo(UUID hallazgoId) {
        return jpa.listarPorHallazgo(hallazgoId);
    }
}
