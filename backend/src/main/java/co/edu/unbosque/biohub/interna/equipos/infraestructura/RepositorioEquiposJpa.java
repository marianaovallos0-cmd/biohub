package co.edu.unbosque.biohub.interna.equipos.infraestructura;

import co.edu.unbosque.biohub.interna.equipos.dominio.EquipoCampo;
import co.edu.unbosque.biohub.interna.equipos.dominio.RepositorioEquipos;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class RepositorioEquiposJpa implements RepositorioEquipos {

    private final EquipoSpringData jpa;

    public RepositorioEquiposJpa(EquipoSpringData jpa) {
        this.jpa = jpa;
    }

    // saveAndFlush: si dos administradores registran el mismo código a la vez, el UNIQUE de la base falla aquí
    @Override
    public void guardar(EquipoCampo equipo) {
        jpa.saveAndFlush(equipo);
    }

    @Override
    public boolean existeCodigo(String codigoIdentificacion) {
        return jpa.existsByCodigoIdentificacion(codigoIdentificacion);
    }

    @Override
    public List<EquipoCampo> listar() {
        return jpa.findAllByOrderByCodigoIdentificacionAsc();
    }

    @Override
    public List<EquipoCampo> listarPorInvestigador(UUID investigadorId) {
        return jpa.findByInvestigadorIdOrderByCodigoIdentificacionAsc(investigadorId);
    }
}
