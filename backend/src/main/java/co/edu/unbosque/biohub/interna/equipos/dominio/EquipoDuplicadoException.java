package co.edu.unbosque.biohub.interna.equipos.dominio;

// HU-04, criterio 3: dos equipos no pueden tener la misma identificación
public class EquipoDuplicadoException extends RuntimeException {

    public EquipoDuplicadoException(String codigo) {
        super("Ya existe un equipo con la identificación «" + codigo + "».");
    }
}
