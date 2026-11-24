package co.edu.unbosque.biohub.publica.consultapublica.dominio;

import java.util.Optional;
import java.util.UUID;

/**
 * Lo que la ficha pública necesita leer. Va aparte de RepositorioConsultaPublica
 * para que cada caso de uso dependa solo de lo que usa (principio I de SOLID).
 * La implementación solo puede usar la vista pública, que no tiene coordenadas.
 */
public interface RepositorioFichaPublica {

    Optional<FichaPublica> buscarPorHallazgo(UUID hallazgoId);
}
