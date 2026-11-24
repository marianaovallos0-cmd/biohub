package co.edu.unbosque.biohub.interna.ubicaciones.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.RepositorioUsuariosFalso;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.ServicioAutorizacion;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.IntentoAccesoNoAutorizado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioIntentos;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.RepositorioConsultaUbicaciones;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.UbicacionConsultada;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.UbicacionNoEncontradaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.rol;
import static co.edu.unbosque.biohub.interna.seguridad.DatosDePrueba.usuario;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.ALTO_RANGO;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.DIRECTOR;
import static co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion.INVESTIGADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicioConsultaUbicacionTest {

    private static final UUID CONDOR = UUID.fromString("40000000-0000-0000-0000-000000000009");

    /** Doble del repositorio: anota qué lectura se usó para comprobar que sin permiso no se leen coordenadas. */
    static class RepositorioFalso implements RepositorioConsultaUbicaciones {
        final List<String> lecturas = new ArrayList<>();

        @Override
        public Optional<UbicacionConsultada> exacta(UUID id) {
            lecturas.add("exacta");
            return id.equals(CONDOR)
                    ? Optional.of(UbicacionConsultada.exacta(CONDOR, "Vultur gryphus", "Andina", "Boyacá", true, 6.0021, -72.3047))
                    : Optional.empty();
        }

        @Override
        public Optional<UbicacionConsultada> publica(UUID id) {
            lecturas.add("publica");
            return id.equals(CONDOR)
                    ? Optional.of(UbicacionConsultada.publica(CONDOR, "Vultur gryphus", "Andina", "Boyacá", true))
                    : Optional.empty();
        }

        @Override
        public List<UbicacionConsultada> todasExactas() {
            lecturas.add("todasExactas");
            return List.of(UbicacionConsultada.exacta(CONDOR, "Vultur gryphus", "Andina", "Boyacá", true, 6.0021, -72.3047));
        }

        @Override
        public List<UbicacionConsultada> todasPublicas() {
            lecturas.add("todasPublicas");
            return List.of(UbicacionConsultada.publica(CONDOR, "Vultur gryphus", "Andina", "Boyacá", true));
        }
    }

    static class IntentosFalso implements RepositorioIntentos {
        final List<IntentoAccesoNoAutorizado> guardados = new ArrayList<>();

        @Override
        public void guardar(IntentoAccesoNoAutorizado intento) {
            guardados.add(intento);
        }

        @Override
        public List<IntentoAccesoNoAutorizado> buscarEntre(OffsetDateTime desde, OffsetDateTime hasta) {
            return guardados;
        }
    }

    private RepositorioFalso ubicaciones;
    private IntentosFalso intentos;
    private ServicioConsultaUbicacion servicio;
    private UsuarioAutenticado sofia;
    private UsuarioAutenticado laura;
    private UsuarioAutenticado jorge;

    @BeforeEach
    void preparar() {
        Usuario altoRango = usuario("Sofía Herrera", "altorango@biohub.test", rol(ALTO_RANGO, "VER_COORDENADAS"));
        Usuario investigador = usuario("Laura Gómez", "investigador1@biohub.test", rol(INVESTIGADOR, "REGISTRAR_HALLAZGO"));
        Usuario director = usuario("Jorge Peña", "director@biohub.test", rol(DIRECTOR, "VER_REPORTES"));
        RepositorioUsuariosFalso usuarios = new RepositorioUsuariosFalso();
        usuarios.guardar(altoRango);
        usuarios.guardar(investigador);
        usuarios.guardar(director);

        ubicaciones = new RepositorioFalso();
        intentos = new IntentosFalso();
        Clock reloj = Clock.fixed(Instant.parse("2026-11-23T20:00:00Z"), ZoneId.of("America/Bogota"));
        servicio = new ServicioConsultaUbicacion(new ServicioAutorizacion(usuarios, intentos, reloj), ubicaciones);

        sofia = UsuarioAutenticado.de(altoRango);
        laura = UsuarioAutenticado.de(investigador);
        jorge = UsuarioAutenticado.de(director);
    }

    // ---------- Criterio 1: alto rango ve coordenadas exactas ----------

    @Test
    void elAltoRangoRecibeLasCoordenadasExactas() {
        UbicacionConsultada u = servicio.obtenerUbicacion(CONDOR, sofia, "GET /x", "10.0.0.1");

        assertThat(u.exacta()).isTrue();
        assertThat(u.latitud()).isEqualTo(6.0021);
        assertThat(u.longitud()).isEqualTo(-72.3047);
        assertThat(intentos.guardados).isEmpty();
    }

    // ---------- Criterios 2 y 4: sin permiso, solo región y departamento ----------

    @Test
    void sinPermisoSoloRecibeRegionYDepartamentoSinNingunaCoordenada() {
        UbicacionConsultada u = servicio.obtenerUbicacion(CONDOR, laura, "GET /x", "10.0.0.1");

        assertThat(u.exacta()).isFalse();
        assertThat(u.region()).isEqualTo("Andina");
        assertThat(u.departamento()).isEqualTo("Boyacá");
        assertThat(u.latitud()).isNull();
        assertThat(u.longitud()).isNull();
    }

    @Test
    void sinPermisoLasCoordenadasNiSiquieraSeLeenDeLaBase() {
        servicio.obtenerUbicacion(CONDOR, laura, "GET /x", "10.0.0.1");
        servicio.listar(jorge, "GET /y", "10.0.0.2");

        assertThat(ubicaciones.lecturas).containsExactly("publica", "todasPublicas");
    }

    @Test
    void sinPermisoElIntentoQuedaRegistrado() {
        servicio.obtenerUbicacion(CONDOR, laura, "GET /interno/hallazgos/" + CONDOR + "/ubicacion", "10.0.0.7");

        assertThat(intentos.guardados).hasSize(1);
        assertThat(intentos.guardados.getFirst().getRecursoSolicitado())
                .isEqualTo("GET /interno/hallazgos/" + CONDOR + "/ubicacion");
        assertThat(intentos.guardados.getFirst().getIpOrigen()).isEqualTo("10.0.0.7");
    }

    // ---------- Criterio 3: el 100 % pasa por la validación ----------

    @Test
    void sinUsuarioNoHayCoordenadas() {
        UbicacionConsultada u = servicio.obtenerUbicacion(CONDOR, null, "GET /x", "10.0.0.1");

        assertThat(u.latitud()).isNull();
        assertThat(ubicaciones.lecturas).containsExactly("publica");
    }

    @Test
    void laListaRespetaLaMismaRegla() {
        assertThat(servicio.listar(sofia, "GET /y", "ip")).allMatch(UbicacionConsultada::exacta);
        assertThat(servicio.listar(laura, "GET /y", "ip"))
                .noneMatch(UbicacionConsultada::exacta)
                .allMatch(u -> u.latitud() == null && u.longitud() == null);
    }

    @Test
    void unHallazgoSinUbicacionLanzaNoEncontrada() {
        assertThatThrownBy(() -> servicio.obtenerUbicacion(UUID.randomUUID(), sofia, "GET /x", "ip"))
                .isInstanceOf(UbicacionNoEncontradaException.class);
    }
}
