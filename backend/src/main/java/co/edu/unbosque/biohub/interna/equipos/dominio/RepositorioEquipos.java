package co.edu.unbosque.biohub.interna.equipos.dominio;

import java.util.List;

public interface RepositorioEquipos {

    void guardar(EquipoCampo equipo);

    boolean existeCodigo(String codigoIdentificacion);

    List<EquipoCampo> listar();
}
