package co.edu.unbosque.biohub.interna.reportes.aplicacion;

import co.edu.unbosque.biohub.interna.reportes.dominio.FilaReporte;
import co.edu.unbosque.biohub.interna.reportes.dominio.Reporte;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Lo que ve el director: el reporte con sus totales y una fila por departamento
public record DetalleReporte(UUID id, String mes, boolean generado, OffsetDateTime fechaGeneracion,
                             int totalHallazgos, int hallazgosProtegidos, int departamentosEnAlerta,
                             List<FilaReporte> filas) {

    // Criterio 2: si no hay filas, los totales quedan en cero
    public static DetalleReporte de(Reporte reporte, List<FilaReporte> filas) {
        return new DetalleReporte(reporte.id(), reporte.mes().toString(), reporte.generado(),
                reporte.fechaGeneracion(),
                filas.stream().mapToInt(FilaReporte::totalHallazgos).sum(),
                filas.stream().mapToInt(FilaReporte::hallazgosProtegidos).sum(),
                (int) filas.stream().filter(FilaReporte::enAlerta).count(),
                filas);
    }
}
