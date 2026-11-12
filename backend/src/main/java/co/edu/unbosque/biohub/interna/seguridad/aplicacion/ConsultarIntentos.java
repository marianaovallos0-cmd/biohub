package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.dominio.RangoFechasInvalidoException;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioIntentos;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ConsultarIntentos {

    private final RepositorioIntentos intentos;
    private final Clock reloj;

    public ConsultarIntentos(RepositorioIntentos intentos, Clock reloj) {
        this.intentos = intentos;
        this.reloj = reloj;
    }

    public List<IntentoRegistrado> ejecutar(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new RangoFechasInvalidoException();
        }
        OffsetDateTime inicio = desde.atStartOfDay(reloj.getZone()).toOffsetDateTime();
        OffsetDateTime fin = hasta.plusDays(1).atStartOfDay(reloj.getZone()).toOffsetDateTime();

        return intentos.buscarEntre(inicio, fin).stream()
                .map(IntentoRegistrado::de)
                .toList();
    }
}