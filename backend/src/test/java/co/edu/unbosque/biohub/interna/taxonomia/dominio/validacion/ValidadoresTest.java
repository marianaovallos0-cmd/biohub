package co.edu.unbosque.biohub.interna.taxonomia.dominio.validacion;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoDato;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

// Strategy: cada TipoDato valida con su propio validador (HU-02, criterio "no se acepta un valor de otro tipo")
class ValidadoresTest {

    @ParameterizedTest(name = "{0} como {1} -> {2}")
    @CsvSource({
            "24.5,              NUMERO,  true",
            "-3,                NUMERO,  true",
            "0.75,              NUMERO,  true",
            "veinte,            NUMERO,  false",
            "'24,5',            NUMERO,  false",
            "NaN,               NUMERO,  false",
            "Canto en la noche, TEXTO,   true",
            "2026-11-13,        FECHA,   true",
            "2026-02-30,        FECHA,   false",
            "13/11/2026,        FECHA,   false",
            "evidencias/a.wav,  ARCHIVO, true",
            "evidencias/a.JPG,  ARCHIVO, true",
            "evidencias/a.exe,  ARCHIVO, false",
            "../etc/passwd.png, ARCHIVO, false",
            "/data/a.png,       ARCHIVO, false",
            "https://x.co/a.png, ARCHIVO, false",
            "sin-extension,     ARCHIVO, false"
    })
    void cadaTipoAceptaSoloSuFormato(String valor, TipoDato tipo, boolean esperado) {
        assertThat(tipo.validador().esValido(valor)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void ningunTipoAceptaUnValorVacio(String valor) {
        for (TipoDato tipo : TipoDato.values()) {
            assertThat(tipo.validador().esValido(valor)).as(tipo.name()).isFalse();
        }
    }

    @ParameterizedTest
    @EnumSource(TipoDato.class)
    void cadaTipoExplicaQueEspera(TipoDato tipo) {
        assertThat(tipo.validador().mensajeError()).isNotBlank();
    }

    @org.junit.jupiter.api.Test
    void unTextoDemasiadoLargoNoSeAcepta() {
        assertThat(TipoDato.TEXTO.validador().esValido("a".repeat(ValidadorTexto.LARGO_MAXIMO + 1))).isFalse();
    }
}
