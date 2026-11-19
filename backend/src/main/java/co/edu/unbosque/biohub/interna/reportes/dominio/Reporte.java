package co.edu.unbosque.biohub.interna.reportes.dominio;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.UUID;

// Un reporte que pidió el director. Si fechaGeneracion es null, la analítica todavía no lo calcula
public record Reporte(UUID id, YearMonth mes, OffsetDateTime fechaGeneracion) {

    public boolean generado() {
        return fechaGeneracion != null;
    }
}
