package co.edu.unbosque.biohub.interna.seguridad.infraestructura;

import co.edu.unbosque.biohub.interna.seguridad.dominio.IntentoAccesoNoAutorizado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioIntentos;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class RepositorioIntentosJpa implements RepositorioIntentos {

    private final IntentoSpringData jpa;

    public RepositorioIntentosJpa(IntentoSpringData jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(IntentoAccesoNoAutorizado intento) {
        jpa.save(intento);
    }

    @Override
    public List<IntentoAccesoNoAutorizado> buscarEntre(OffsetDateTime desde, OffsetDateTime hasta) {
            return jpa.findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDesc(desde, hasta);
    }
}