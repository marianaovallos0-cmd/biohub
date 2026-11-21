package co.edu.unbosque.biohub.interna.historial.dominio;

import java.util.List;
import java.util.UUID;

// Solo se insertan registros: nunca se editan ni se borran (ASR-5)
public interface RepositorioCambios {

    // Guarda el cambio y escribe en la base de una vez. Si la versión nueva choca con otra
    // del mismo número lanza VersionDuplicadaException.
    void guardar(RegistroCambio cambio);

    // Los cambios de un hallazgo, del más antiguo al más reciente
    List<RegistroCambio> listarPorHallazgo(UUID hallazgoId);
}
