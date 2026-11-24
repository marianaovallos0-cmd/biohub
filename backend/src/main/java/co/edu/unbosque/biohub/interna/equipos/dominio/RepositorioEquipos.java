package co.edu.unbosque.biohub.interna.equipos.dominio;

import java.util.List;
import java.util.UUID;

public interface RepositorioEquipos {

    void guardar(EquipoCampo equipo);

    boolean existeCodigo(String codigoIdentificacion);

    List<EquipoCampo> listar();

    // HU-05: los equipos de los que responde un investigador
    List<EquipoCampo> listarPorInvestigador(UUID investigadorId);
}
