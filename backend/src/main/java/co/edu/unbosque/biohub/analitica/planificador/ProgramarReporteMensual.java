package co.edu.unbosque.biohub.analitica.planificador;

import co.edu.unbosque.biohub.analitica.generador.dominio.ProgramadorReportes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;

/**
 * HU-19: el día 1 de cada mes a las 2:00 a. m. (hora de Colombia, baja carga) calcula el mes anterior.
 * Corre en el contenedor de analítica: no usa la memoria ni el procesador de las APIs,
 * y nadie espera la respuesta. Lo único compartido es PostgreSQL.
 *
 * Para la demostración se puede cambiar la hora con BIOHUB_ANALITICA_CRON_MENSUAL
 * (por ejemplo "0 * * * * *" para que corra cada minuto).
 */
@Component
public class ProgramarReporteMensual {

    public static final String CRON = "${biohub.analitica.cron-mensual:0 0 2 1 * *}";
    public static final String ZONA = "America/Bogota";

    private static final Logger log = LoggerFactory.getLogger(ProgramarReporteMensual.class);

    private final ProgramadorReportes programador;
    private final RevisarReportesPendientes revisar;
    private final Clock reloj;

    public ProgramarReporteMensual(ProgramadorReportes programador, RevisarReportesPendientes revisar, Clock reloj) {
        this.programador = programador;
        this.revisar = revisar;
        this.reloj = reloj;
    }

    @Scheduled(cron = CRON, zone = ZONA)
    public void ejecutar() {
        YearMonth mesAnterior = YearMonth.now(reloj).minusMonths(1);
        if (programador.programar(mesAnterior)) {
            log.info("Reporte mensual de {} programado", mesAnterior);
        } else {
            log.info("El reporte de {} ya estaba pedido; no se duplica", mesAnterior);
        }
        // Se calcula de una vez con la misma lógica de los reportes que pide el director (HU-18)
        revisar.revisar();
    }
}
