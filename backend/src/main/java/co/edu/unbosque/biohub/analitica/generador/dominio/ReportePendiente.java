package co.edu.unbosque.biohub.analitica.generador.dominio;

import java.time.YearMonth;
import java.util.UUID;

// Un reporte que el director pidió y la analítica todavía no ha calculado
public record ReportePendiente(UUID id, YearMonth mes) {
}