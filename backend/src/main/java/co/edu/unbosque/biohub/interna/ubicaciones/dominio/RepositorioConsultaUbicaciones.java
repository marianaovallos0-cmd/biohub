package co.edu.unbosque.biohub.interna.ubicaciones.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dos lecturas separadas a propósito (ASR-1): las "publicas" ni siquiera traen
 * latitud y longitud de la base. Así, si la validación de permisos falla, las
 * coordenadas nunca llegan a la memoria de la api.
 */
public interface RepositorioConsultaUbicaciones {

    Optional<UbicacionConsultada> exacta(UUID hallazgoId);

    Optional<UbicacionConsultada> publica(UUID hallazgoId);

    List<UbicacionConsultada> todasExactas();

    List<UbicacionConsultada> todasPublicas();
}
