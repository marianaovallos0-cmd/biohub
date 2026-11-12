package co.edu.unbosque.biohub.interna.seguridad;

import co.edu.unbosque.biohub.interna.seguridad.dominio.IntentoAccesoNoAutorizado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioIntentos;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Doble tipo fake: guarda los intentos en una lista en vez de PostgreSQL
public class RepositorioIntentosFalso implements RepositorioIntentos {

    public final List<IntentoAccesoNoAutorizado> guardados = new ArrayList<>();

    @Override
    public void guardar(IntentoAccesoNoAutorizado intento) {
        guardados.add(intento);
    }

    @Override
    public List<IntentoAccesoNoAutorizado> buscarEntre(OffsetDateTime desde, OffsetDateTime hasta) {
        return guardados.stream()
                .filter(i -> !i.getFechaHora().isBefore(desde) && i.getFechaHora().isBefore(hasta))
                .sorted(Comparator.comparing(IntentoAccesoNoAutorizado::getFechaHora).reversed())
                .toList();
    }
}
