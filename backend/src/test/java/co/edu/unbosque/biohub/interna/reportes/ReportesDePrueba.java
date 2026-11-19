package co.edu.unbosque.biohub.interna.reportes;

import co.edu.unbosque.biohub.interna.reportes.dominio.FilaReporte;
import co.edu.unbosque.biohub.interna.reportes.dominio.Reporte;
import co.edu.unbosque.biohub.interna.reportes.dominio.RepositorioReportes;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Doble tipo fake de RepositorioReportes: guarda en memoria en vez de PostgreSQL
public class ReportesDePrueba implements RepositorioReportes {

    public final List<Reporte> reportes = new ArrayList<>();
    public final Map<UUID, List<FilaReporte>> filas = new HashMap<>();

    @Override
    public Optional<Reporte> buscarPorMes(YearMonth mes) {
        return reportes.stream().filter(r -> r.mes().equals(mes)).findFirst();
    }

    @Override
    public Optional<Reporte> buscarPorId(UUID id) {
        return reportes.stream().filter(r -> r.id().equals(id)).findFirst();
    }

    @Override
    public List<Reporte> listar() {
        return reportes;
    }

    @Override
    public Reporte crearPendiente(YearMonth mes, UUID solicitadoPor) {
        Reporte reporte = new Reporte(UUID.randomUUID(), mes, null);
        reportes.add(reporte);
        return reporte;
    }

    @Override
    public List<FilaReporte> filas(UUID reporteId) {
        return filas.getOrDefault(reporteId, List.of());
    }
}
