package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import co.edu.unbosque.biohub.interna.hallazgos.dominio.AlmacenArchivos;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Evidencia;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Hallazgo;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.HallazgoInvalidoException;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.ReglasArchivos;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioEvidencias;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioHallazgos;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.DatosFicha;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.ServicioFichas;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaInvalidaException;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaTaxonomica;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.RepositorioUbicaciones;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.Ubicacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// HU-01. Registra el hallazgo con su ficha (versión 1), su ubicación y sus evidencias.
@Service
public class RegistrarHallazgo {

    // 5 de 10 MB más los archivos de la ficha caben en los 60 MB que dejan pasar Nginx y Spring
    static final int MAXIMO_EVIDENCIAS = 5;

    private static final Logger log = LoggerFactory.getLogger(RegistrarHallazgo.class);

    private final RepositorioHallazgos hallazgos;
    private final RepositorioEvidencias evidencias;
    private final RepositorioUbicaciones ubicaciones;
    private final ServicioFichas fichas;
    private final AlmacenArchivos almacen;
    private final Clock reloj;

    public RegistrarHallazgo(RepositorioHallazgos hallazgos, RepositorioEvidencias evidencias,
                             RepositorioUbicaciones ubicaciones, ServicioFichas fichas,
                             AlmacenArchivos almacen, Clock reloj) {
        this.hallazgos = hallazgos;
        this.evidencias = evidencias;
        this.ubicaciones = ubicaciones;
        this.fichas = fichas;
        this.almacen = almacen;
        this.reloj = reloj;
    }

    @Transactional
    public HallazgoRegistrado ejecutar(SolicitudRegistro solicitud, UsuarioAutenticado autor) {
        if (autor == null) {
            throw new IllegalArgumentException("El hallazgo necesita un autor.");
        }
        long inicio = System.nanoTime();
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        List<String> errores = new ArrayList<>();

        // 1. Validar
        if (!Hallazgo.expedicionValida(solicitud.expedicion())) {
            errores.add(Hallazgo.errorExpedicion());
        }

        DatosUbicacion ubicacion = solicitud.ubicacion();
        if (ubicacion == null) {
            errores.add("Falta la ubicación del hallazgo.");
        } else {
            errores.addAll(Ubicacion.errores(ubicacion.latitud(), ubicacion.longitud(),
                    ubicacion.region(), ubicacion.departamento()));
        }

        List<EvidenciaSubida> evidenciasSubidas = solicitud.evidencias();
        if (evidenciasSubidas.size() > MAXIMO_EVIDENCIAS) {
            errores.add("Se pueden adjuntar máximo " + MAXIMO_EVIDENCIAS + " evidencias por hallazgo.");
        }
        List<PorGuardar> evidenciasPorGuardar = new ArrayList<>();
        for (int i = 0; i < evidenciasSubidas.size(); i++) {
            EvidenciaSubida evidencia = evidenciasSubidas.get(i);
            ArchivoSubido archivo = evidencia.archivo();
            String etiqueta = "Evidencia " + (i + 1) + (archivo == null ? "" : " (" + archivo.nombreOriginal() + ")");
            if (archivo == null) {
                errores.add(etiqueta + ": falta el archivo.");
                continue;
            }
            List<String> erroresEvidencia = ReglasArchivos.erroresDeEvidencia(
                    etiqueta, evidencia.tipo(), archivo.nombreOriginal(), archivo.tamanoBytes());
            if (evidencia.descripcion() != null && evidencia.descripcion().trim().length() > Evidencia.LARGO_MAXIMO_DESCRIPCION) {
                erroresEvidencia.add(etiqueta + ": la descripción tiene más de " + Evidencia.LARGO_MAXIMO_DESCRIPCION + " caracteres.");
            }
            errores.addAll(erroresEvidencia);
            if (erroresEvidencia.isEmpty()) {
                evidenciasPorGuardar.add(new PorGuardar(evidencia,
                        almacen.nuevaRuta(ReglasArchivos.extension(archivo.nombreOriginal()))));
            }
        }

        // Los atributos de tipo ARCHIVO guardan la ruta del archivo como valor; el formato lo valida la ficha
        DatosFicha datosFicha = solicitud.ficha();
        Map<String, ArchivoSubido> archivosPorRuta = new LinkedHashMap<>();
        Map<UUID, String> rutasAtributos = new LinkedHashMap<>();
        for (Map.Entry<UUID, ArchivoSubido> entrada : solicitud.archivosAtributos().entrySet()) {
            ArchivoSubido archivo = entrada.getValue();
            List<String> erroresTamano = ReglasArchivos.erroresDeTamano("Archivo " + archivo.nombreOriginal(), archivo.tamanoBytes());
            errores.addAll(erroresTamano);
            if (erroresTamano.isEmpty() && datosFicha != null) {
                String ruta = almacen.nuevaRuta(ReglasArchivos.extension(archivo.nombreOriginal()));
                archivosPorRuta.put(ruta, archivo);
                rutasAtributos.put(entrada.getKey(), ruta);
            }
        }

        Hallazgo hallazgo = new Hallazgo(
                Hallazgo.expedicionValida(solicitud.expedicion()) ? solicitud.expedicion() : null, autor.id(), ahora);
        FichaTaxonomica ficha = null;
        try {
            ficha = fichas.armar(hallazgo.getId(), 1, datosFicha, rutasAtributos);
        } catch (FichaInvalidaException e) {
            errores.addAll(e.getErrores());
        }

        if (!errores.isEmpty()) {
            throw new HallazgoInvalidoException(errores);
        }

        // 2. Escribir (en este orden por las llaves foráneas)
        hallazgos.guardar(hallazgo);
        fichas.guardar(ficha);
        ubicaciones.guardar(new Ubicacion(hallazgo.getId(), ubicacion.latitud(), ubicacion.longitud(),
                ubicacion.region(), ubicacion.departamento(), ubicacion.especieProtegida()));

        archivosPorRuta.forEach((ruta, archivo) -> guardarArchivo(ruta, archivo));
        List<EvidenciaRegistrada> registradas = new ArrayList<>();
        for (PorGuardar porGuardar : evidenciasPorGuardar) {
            EvidenciaSubida evidencia = porGuardar.evidencia();
            guardarArchivo(porGuardar.ruta(), evidencia.archivo());
            Evidencia guardada = new Evidencia(hallazgo.getId(), evidencia.tipo(), porGuardar.ruta(),
                    evidencia.descripcion(), ahora);
            evidencias.guardar(guardada);
            registradas.add(EvidenciaRegistrada.de(guardada, evidencia.archivo().nombreOriginal()));
        }

        // Criterio 4 (menos de 5 s): queda en el log de api-interna, visible en Docker Desktop
        log.info("Hallazgo {} registrado por {} en {} ms", hallazgo.getId(), autor.correo(),
                (System.nanoTime() - inicio) / 1_000_000);

        return new HallazgoRegistrado(hallazgo.getId(), hallazgo.getFechaRegistro(), autor.nombre(),
                ficha.getGenero() + " " + ficha.getEspecie(), registradas);
    }

    private record PorGuardar(EvidenciaSubida evidencia, String ruta) { }

    private void guardarArchivo(String ruta, ArchivoSubido archivo) {
        try (InputStream contenido = archivo.contenido().abrir()) {
            almacen.guardar(ruta, contenido);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo " + archivo.nombreOriginal(), e);
        }
    }
}
