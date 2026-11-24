package co.edu.unbosque.biohub.analitica.generador.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class IndicadorDepartamentoTest {

    private static IndicadorDepartamento de(int anterior, int actual) {
        return IndicadorDepartamento.calcular(
                new ConteoDepartamento("Chocó", actual, 0),
                new ConteoDepartamento("Chocó", anterior, 0));
    }

    // Valores límite: −30 % justo entra en alerta, −20 % no
    @Test
    void unaCaidaDe30PorCientoQuedaEnAlerta() {
        IndicadorDepartamento indicador = de(10, 7);

        assertThat(indicador.variacionPorcentual()).isEqualByComparingTo("-30");
        assertThat(indicador.enAlerta()).isTrue();
    }

    @Test
    void unaCaidaMenorNoQuedaEnAlerta() {
        assertThat(de(10, 8).enAlerta()).isFalse();
    }

    @Test
    void siNoQuedoNingunHallazgoEsMenos100YQuedaEnAlerta() {
        IndicadorDepartamento indicador = de(4, 0);

        assertThat(indicador.variacionPorcentual()).isEqualByComparingTo("-100");
        assertThat(indicador.enAlerta()).isTrue();
    }

    @Test
    void siElMesAnteriorNoTuvoHallazgosNoHayVariacionNiAlerta() {
        IndicadorDepartamento indicador = de(0, 5);

        assertThat(indicador.variacionPorcentual()).isNull();
        assertThat(indicador.enAlerta()).isFalse();
    }

    @Test
    void laVariacionSeRedondeaADosDecimales() {
        assertThat(de(3, 4).variacionPorcentual()).isEqualTo(new BigDecimal("33.33"));
    }
}