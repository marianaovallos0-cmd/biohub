package co.edu.unbosque.biohub.interna.equipos.aplicacion;

import co.edu.unbosque.biohub.interna.equipos.EquiposDePrueba.DirectorioFalso;
import co.edu.unbosque.biohub.interna.equipos.EquiposDePrueba.RepositorioEquiposFalso;
import co.edu.unbosque.biohub.interna.equipos.dominio.InvestigadorNoEncontradoException;
import co.edu.unbosque.biohub.interna.equipos.dominio.TipoEquipo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static co.edu.unbosque.biohub.interna.equipos.EquiposDePrueba.ANDRES;
import static co.edu.unbosque.biohub.interna.equipos.EquiposDePrueba.LAURA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// HU-05 con los datos de prueba: Laura tiene equipos y Andrés no
class ConsultarEquiposDeInvestigadorTest {

    private final RepositorioEquiposFalso equipos = new RepositorioEquiposFalso();
    private final DirectorioFalso investigadores = new DirectorioFalso();
    private final ConsultarEquiposDeInvestigador consultar = new ConsultarEquiposDeInvestigador(equipos, investigadores);

    @BeforeEach
    void preparar() {
        RegistrarEquipo registrar = new RegistrarEquipo(equipos, investigadores);
        registrar.ejecutar(new NuevoEquipo("HID-003", TipoEquipo.HIDROFONO, LAURA.id()));
        registrar.ejecutar(new NuevoEquipo("DRN-001", TipoEquipo.DRON, LAURA.id()));
        registrar.ejecutar(new NuevoEquipo("CAM-014", TipoEquipo.CAMARA_TRAMPA, LAURA.id()));
    }

    // Criterio 1: se listan los equipos asociados a él con su identificación
    @Test
    void listaLosEquiposDelInvestigadorConSuIdentificacion() {
        EquiposDeInvestigador resultado = consultar.ejecutar(LAURA.id());

        assertThat(resultado.investigador().nombre()).isEqualTo("Laura Gómez");
        assertThat(resultado.equipos()).extracting(EquipoRegistrado::codigoIdentificacion)
                .containsExactly("CAM-014", "DRN-001", "HID-003");
        assertThat(resultado.equipos()).extracting(EquipoRegistrado::responsable).containsOnly("Laura Gómez");
    }

    // Criterio 2: sin equipos devuelve la lista vacía, con el investigador para que el frontend lo explique
    @Test
    void unInvestigadorSinEquiposDevuelveLaListaVacia() {
        EquiposDeInvestigador resultado = consultar.ejecutar(ANDRES.id());

        assertThat(resultado.investigador().nombre()).isEqualTo("Andrés Rojas");
        assertThat(resultado.equipos()).isEmpty();
    }

    @Test
    void noMezclaLosEquiposDeOtroInvestigador() {
        new RegistrarEquipo(equipos, investigadores)
                .ejecutar(new NuevoEquipo("SEN-001", TipoEquipo.SENSOR, ANDRES.id()));

        assertThat(consultar.ejecutar(ANDRES.id()).equipos())
                .extracting(EquipoRegistrado::codigoIdentificacion).containsExactly("SEN-001");
        assertThat(consultar.ejecutar(LAURA.id()).equipos()).hasSize(3);
    }

    @Test
    void unInvestigadorQueNoExisteSeRechaza() {
        assertThatThrownBy(() -> consultar.ejecutar(UUID.randomUUID()))
                .isInstanceOf(InvestigadorNoEncontradoException.class);
    }
}
