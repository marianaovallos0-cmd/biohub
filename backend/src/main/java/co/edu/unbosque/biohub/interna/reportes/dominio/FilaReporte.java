package co.edu.unbosque.biohub.interna.reportes.dominio;

import java.math.BigDecimal;

// Una fila de reporte_presion, tal como la dejó la analítica
public record FilaReporte(String departamento, int totalHallazgos, int hallazgosProtegidos,
                          BigDecimal variacionPorcentual, boolean enAlerta) {
}
