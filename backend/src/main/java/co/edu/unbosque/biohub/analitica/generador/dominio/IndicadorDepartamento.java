package co.edu.unbosque.biohub.analitica.generador.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Una fila del reporte de presión ecológica (HU-18)
public record IndicadorDepartamento(String departamento, int totalHallazgos, int hallazgosProtegidos,
                                    BigDecimal variacionPorcentual, boolean enAlerta) {

    // Si los hallazgos caen 30 % o más frente al mes anterior, el departamento queda en alerta
    public static final BigDecimal UMBRAL_ALERTA = new BigDecimal("-30");

    public static IndicadorDepartamento calcular(ConteoDepartamento actual, ConteoDepartamento anterior) {
        BigDecimal variacion = variacion(actual.total(), anterior.total());
        boolean alerta = variacion != null && variacion.compareTo(UMBRAL_ALERTA) <= 0;
        return new IndicadorDepartamento(actual.departamento(), actual.total(), actual.protegidos(),
                variacion, alerta);
    }

    // Sin hallazgos el mes anterior no hay contra qué comparar (no se puede dividir por cero)
    private static BigDecimal variacion(int actual, int anterior) {
        if (anterior == 0) {
            return null;
        }
        return BigDecimal.valueOf(actual - anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(anterior), 2, RoundingMode.HALF_UP);
    }
}