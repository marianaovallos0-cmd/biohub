package co.edu.unbosque.biohub.interna.reportes.aplicacion;

import co.edu.unbosque.biohub.interna.reportes.dominio.Reporte;

import java.time.OffsetDateTime;
import java.util.UUID;

// Lo que sale en la lista de reportes del director
public record ResumenReporte(UUID id, String mes, boolean generado, OffsetDateTime fechaGeneracion) {

    public static ResumenReporte de(Reporte reporte) {
        return new ResumenReporte(reporte.id(), reporte.mes().toString(), reporte.generado(), reporte.fechaGeneracion());
    }
}
