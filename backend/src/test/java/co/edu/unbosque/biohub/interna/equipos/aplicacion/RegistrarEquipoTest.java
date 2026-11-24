package co.edu.unbosque.biohub.interna.equipos.aplicacion;

import co.edu.unbosque.biohub.interna.equipos.EquiposDePrueba.DirectorioFalso;
import co.edu.unbosque.biohub.interna.equipos.EquiposDePrueba.RepositorioEquiposFalso;
import co.edu.unbosque.biohub.interna.equipos.dominio.EquipoCampo;
import co.edu.unbosque.biohub.interna.equipos.dominio.EquipoDuplicadoException;
import co.edu.unbosque.biohub.interna.equipos.dominio.EquipoInvalidoException;
import co.edu.unbosque.biohub.interna.equipos.dominio.TipoEquipo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static co.edu.unbosque.biohub.interna.equipos.EquiposDePrueba.ANDRES;
import static co.edu.unbosque.biohub.interna.equipos.EquiposDePrueba.LAURA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrarEquipoTest {

    private final RepositorioEquiposFalso equipos = new RepositorioEquiposFalso();
    private final DirectorioFalso investigadores = new DirectorioFalso();
    private final RegistrarEquipo registrar = new RegistrarEquipo(equipos, investigadores);

    // Criterio 1: queda creado con su identificación y su investigador responsable
    @ParameterizedTest
    @EnumSource(TipoEquipo.class)
    void registraCualquierTipoDeEquipoConSuResponsable(TipoEquipo tipo) {
        EquipoRegistrado registrado = registrar.ejecutar(new NuevoEquipo("EQ-" + tipo.name(), tipo, LAURA.id()));

        EquipoCampo guardado = equipos.guardados.get(0);
        assertThat(guardado.getCodigoIdentificacion()).isEqualTo("EQ-" + tipo.name());
        assertThat(guardado.getTipo()).isEqualTo(tipo);
        assertThat(guardado.getInvestigadorId()).isEqualTo(LAURA.id());
        assertThat(guardado.getEstado()).isEqualTo("ACTIVO");
        assertThat(registrado.responsable()).isEqualTo("Laura Gómez");
        assertThat(registrado.id()).isNotNull();
    }

    @Test
    void unInvestigadorPuedeTenerVariosEquipos() {
        registrar.ejecutar(new NuevoEquipo("DRN-010", TipoEquipo.DRON, LAURA.id()));
        registrar.ejecutar(new NuevoEquipo("CAM-020", TipoEquipo.CAMARA_TRAMPA, LAURA.id()));

        assertThat(equipos.guardados).extracting(EquipoCampo::getInvestigadorId).containsOnly(LAURA.id());
    }

    // Criterio 2: sin investigador responsable no se guarda
    @Test
    void sinResponsableNoSeGuarda() {
        EquipoInvalidoException error = assertThrows(EquipoInvalidoException.class,
                () -> registrar.ejecutar(new NuevoEquipo("DRN-010", TipoEquipo.DRON, null)));

        assertThat(error.getErrores()).containsExactly("Falta el investigador responsable.");
        assertThat(equipos.guardados).isEmpty();
    }

    @Test
    void elResponsableDebeSerUnInvestigadorActivo() {
        EquipoInvalidoException error = assertThrows(EquipoInvalidoException.class,
                () -> registrar.ejecutar(new NuevoEquipo("DRN-010", TipoEquipo.DRON, UUID.randomUUID())));

        assertThat(error.getErrores()).containsExactly("El responsable debe ser un investigador activo.");
        assertThat(equipos.guardados).isEmpty();
    }

    // Criterio 3: con la misma identificación se rechaza, aunque cambien mayúsculas o espacios
    @Test
    void unCodigoRepetidoSeRechaza() {
        registrar.ejecutar(new NuevoEquipo("DRN-001", TipoEquipo.DRON, LAURA.id()));

        assertThatThrownBy(() -> registrar.ejecutar(new NuevoEquipo("  drn-001 ", TipoEquipo.SENSOR, ANDRES.id())))
                .isInstanceOf(EquipoDuplicadoException.class)
                .hasMessage("Ya existe un equipo con la identificación «DRN-001».");
        assertThat(equipos.guardados).hasSize(1);
    }

    @Test
    void reuneTodosLosErroresEnUnaSolaRespuesta() {
        EquipoInvalidoException error = assertThrows(EquipoInvalidoException.class,
                () -> registrar.ejecutar(new NuevoEquipo("  ", null, null)));

        assertThat(error.getErrores()).containsExactly(
                "Falta el código de identificación del equipo.",
                "Falta el tipo de equipo.",
                "Falta el investigador responsable.");
    }

    // Valor límite: 50 caracteres sí, 51 no
    @Test
    void elCodigoPuedeTenerHasta50Caracteres() {
        assertThat(registrar.ejecutar(new NuevoEquipo("A".repeat(50), TipoEquipo.SENSOR, LAURA.id()))).isNotNull();
        assertThatThrownBy(() -> registrar.ejecutar(new NuevoEquipo("B".repeat(51), TipoEquipo.SENSOR, LAURA.id())))
                .isInstanceOf(EquipoInvalidoException.class);
    }

    @Test
    void laPantallaVeLosEquiposConElNombreDelResponsable() {
        registrar.ejecutar(new NuevoEquipo("HID-003", TipoEquipo.HIDROFONO, ANDRES.id()));
        registrar.ejecutar(new NuevoEquipo("CAM-014", TipoEquipo.CAMARA_TRAMPA, LAURA.id()));

        ConsultarEquipos consultar = new ConsultarEquipos(equipos, investigadores);

        assertThat(consultar.todos()).extracting(EquipoRegistrado::codigoIdentificacion, EquipoRegistrado::responsable)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("CAM-014", "Laura Gómez"),
                        org.assertj.core.groups.Tuple.tuple("HID-003", "Andrés Rojas"));
        assertThat(consultar.responsables()).hasSize(2);
    }
}
