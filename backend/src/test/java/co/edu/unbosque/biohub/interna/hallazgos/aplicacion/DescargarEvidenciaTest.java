package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba;
import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.AlmacenArchivosFalso;
import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.RepositorioEvidenciasFalso;
import co.edu.unbosque.biohub.interna.hallazgos.HallazgosDePrueba.RepositorioHallazgosFalso;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.AccesoEvidenciaDenegadoException;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Evidencia;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.EvidenciaNoEncontradaException;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Hallazgo;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DescargarEvidenciaTest {

    private final RepositorioEvidenciasFalso evidencias = new RepositorioEvidenciasFalso();
    private final RepositorioHallazgosFalso hallazgos = new RepositorioHallazgosFalso();
    private final AlmacenArchivosFalso almacen = new AlmacenArchivosFalso();
    private final DescargarEvidencia descargar = new DescargarEvidencia(evidencias, hallazgos, almacen);

    private final UsuarioAutenticado laura = usuario(NivelAutorizacion.INVESTIGADOR, "REGISTRAR_HALLAZGO");
    private final UsuarioAutenticado andres = usuario(NivelAutorizacion.INVESTIGADOR, "REGISTRAR_HALLAZGO");
    private final UsuarioAutenticado sofia = usuario(NivelAutorizacion.ALTO_RANGO, "VER_COORDENADAS");
    private Evidencia foto;

    private static UsuarioAutenticado usuario(NivelAutorizacion rol, String permiso) {
        return new UsuarioAutenticado(UUID.randomUUID(), rol.name(), rol.name() + "@biohub.test", rol, Set.of(permiso));
    }

    @BeforeEach
    void preparar() {
        Hallazgo hallazgo = new Hallazgo(null, laura.id(), OffsetDateTime.parse("2026-11-13T10:00-05:00"));
        hallazgos.guardar(hallazgo);
        almacen.guardar("evidencias/foto-1.jpg", new ByteArrayInputStream("foto".getBytes()));
        foto = HallazgosDePrueba.evidencia(hallazgo.getId(), "evidencias/foto-1.jpg");
        evidencias.guardar(foto);
    }

    @Test
    void elAutorVeSuEvidencia() throws Exception {
        ArchivoEvidencia archivo = descargar.ejecutar(foto.getId(), laura);

        assertThat(archivo.nombre()).isEqualTo("foto-1.jpg");
        assertThat(archivo.tipoContenido()).isEqualTo("image/jpeg");
        assertThat(new String(archivo.contenido().readAllBytes())).isEqualTo("foto");
    }

    @Test
    void elAltoRangoVeLaEvidencia() {
        assertThat(descargar.ejecutar(foto.getId(), sofia).nombre()).isEqualTo("foto-1.jpg");
    }

    // La foto puede traer coordenadas GPS: otro investigador no la ve
    @Test
    void otroInvestigadorNoVeLaEvidencia() {
        assertThatThrownBy(() -> descargar.ejecutar(foto.getId(), andres))
                .isInstanceOf(AccesoEvidenciaDenegadoException.class);
    }

    @Test
    void unaEvidenciaQueNoExisteDa404() {
        assertThatThrownBy(() -> descargar.ejecutar(UUID.randomUUID(), laura))
                .isInstanceOf(EvidenciaNoEncontradaException.class);
    }

    @Test
    void siElArchivoNoEstaEnElDiscoTambienDa404() {
        Evidencia sinArchivo = HallazgosDePrueba.evidencia(foto.getHallazgoId(), "evidencias/no-esta.jpg");
        evidencias.guardar(sinArchivo);

        assertThatThrownBy(() -> descargar.ejecutar(sinArchivo.getId(), laura))
                .isInstanceOf(EvidenciaNoEncontradaException.class);
    }
}
