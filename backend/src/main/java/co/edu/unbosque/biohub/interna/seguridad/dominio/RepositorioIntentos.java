package co.edu.unbosque.biohub.interna.seguridad.dominio;

import java.time.OffsetDateTime;
import java.util.List;

public interface RepositorioIntentos {

    void guardar(IntentoAccesoNoAutorizado intento);

    List<IntentoAccesoNoAutorizado> buscarEntre(OffsetDateTime desde, OffsetDateTime hasta);
}