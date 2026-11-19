package co.edu.unbosque.biohub.analitica.planificador;

import co.edu.unbosque.biohub.analitica.generador.aplicacion.GenerarReportePresion;
import co.edu.unbosque.biohub.analitica.generador.dominio.IndicadorDepartamento;
import co.edu.unbosque.biohub.analitica.generador.dominio.ReportePendiente;
import co.edu.unbosque.biohub.analitica.generador.dominio.RepositorioAnalitico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

// HU-18: cada minuto calcula los reportes que pidió el director. La tarea mensual automática es de HU-19
@Component
public class RevisarReportesPendientes {

    private static final Logger log = LoggerFactory.getLogger(RevisarReportesPendientes.class);

    private final RepositorioAnalitico repositorio;
    private final GenerarReportePresion generar;

    public RevisarReportesPendientes(RepositorioAnalitico repositorio, GenerarReportePresion generar) {
        this.repositorio = repositorio;
        this.generar = generar;
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void revisar() {
        for (ReportePendiente pendiente : repositorio.pendientes()) {
            try {
                List<IndicadorDepartamento> filas = generar.generar(pendiente);
                log.info("Reporte {} de {} generado con {} departamentos", pendiente.id(), pendiente.mes(), filas.size());
            } catch (RuntimeException e) {
                // Si uno falla, los demás siguen. Como sigue pendiente, se intenta otra vez en la próxima revisión
                log.error("No se pudo generar el reporte {} de {}", pendiente.id(), pendiente.mes(), e);
            }
        }
    }
}