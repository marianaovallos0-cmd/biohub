package co.edu.unbosque.biohub.interna.ubicaciones.infraestructura;

import co.edu.unbosque.biohub.interna.ubicaciones.dominio.RepositorioUbicaciones;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.Ubicacion;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioUbicacionesJpa implements RepositorioUbicaciones {

    private final UbicacionSpringData jpa;

    public RepositorioUbicacionesJpa(UbicacionSpringData jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(Ubicacion ubicacion) {
        jpa.save(ubicacion);
    }
}
