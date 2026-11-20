package co.edu.unbosque.biohub.interna.reportes.dominio;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioReportes {

    Optional<Reporte> buscarPorMes(YearMonth mes);

    Optional<Reporte> buscarPorId(UUID id);

    // Del mes más reciente al más antiguo
    List<Reporte> listar();

    // Lo deja pendiente para que lo calcule la analítica
    Reporte crearPendiente(YearMonth mes, UUID solicitadoPor);

    List<FilaReporte> filas(UUID reporteId);
}
