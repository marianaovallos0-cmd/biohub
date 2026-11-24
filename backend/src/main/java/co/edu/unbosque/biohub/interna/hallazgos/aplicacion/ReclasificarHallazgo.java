package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import co.edu.unbosque.biohub.interna.hallazgos.dominio.HallazgoNoEncontradoException;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.ReclasificacionInvalidaException;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioHallazgos;
import co.edu.unbosque.biohub.interna.historial.dominio.RegistroCambio;
import co.edu.unbosque.biohub.interna.historial.dominio.RepositorioCambios;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.DatosFicha;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.ServicioFichas;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaInvalidaException;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaTaxonomica;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoDato;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.ValorAtributo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// HU-06 y HU-07. Crea la siguiente versión de la ficha y el registro de quién la autorizó.
// Las dos cosas se guardan en una sola transacción: quedan las dos o ninguna (ASR-4 y ASR-5).
// La versión anterior no se toca: las fichas solo se insertan.
@Service
public class ReclasificarHallazgo {

    private static final Logger log = LoggerFactory.getLogger(ReclasificarHallazgo.class);

    private final RepositorioHallazgos hallazgos;
    private final ServicioFichas fichas;
    private final RepositorioCambios cambios;
    private final Clock reloj;

    public ReclasificarHallazgo(RepositorioHallazgos hallazgos, ServicioFichas fichas,
                                RepositorioCambios cambios, Clock reloj) {
        this.hallazgos = hallazgos;
        this.fichas = fichas;
        this.cambios = cambios;
        this.reloj = reloj;
    }

    @Transactional
    public ReclasificacionRegistrada ejecutar(UUID hallazgoId, SolicitudReclasificacion solicitud,
                                              UsuarioAutenticado auditor) {
        hallazgos.buscarPorId(hallazgoId).orElseThrow(HallazgoNoEncontradoException::new);
        FichaTaxonomica vigente = fichas.vigente(hallazgoId).orElseThrow(HallazgoNoEncontradoException::new);
        OffsetDateTime ahora = OffsetDateTime.now(reloj);

        // HU-07, criterio 1: sin justificación o sin autorizador no se guarda nada
        List<String> errores = new ArrayList<>(
                RegistroCambio.errores(solicitud.justificacion(), auditor == null ? null : auditor.id()));

        FichaTaxonomica nueva = null;
        try {
            nueva = fichas.armar(hallazgoId, vigente.getNumeroVersion() + 1, solicitud.ficha(),
                    archivosHeredados(vigente, solicitud.ficha()));
        } catch (FichaInvalidaException e) {
            errores.addAll(e.getErrores());
        }
        if (nueva != null && mismaClasificacion(vigente, nueva)) {
            errores.add("La clasificación nueva es igual a la vigente. Cambia algún dato para reclasificar.");
        }
        if (!errores.isEmpty()) {
            throw new ReclasificacionInvalidaException(errores);
        }

        // La versión nueva primero (la llave foránea del registro apunta a ella)
        fichas.guardar(nueva);
        cambios.guardar(new RegistroCambio(ahora, solicitud.justificacion(), auditor.id(),
                vigente.getId(), nueva.getId()));

        log.info("Hallazgo {} reclasificado de v{} a v{} por {}", hallazgoId,
                vigente.getNumeroVersion(), nueva.getNumeroVersion(), auditor.correo());

        return new ReclasificacionRegistrada(hallazgoId, vigente.getNumeroVersion(), nueva.getNumeroVersion(),
                nombre(vigente), nombre(nueva), ahora, auditor.nombre());
    }

    // Los atributos ARCHIVO no se suben de nuevo al reclasificar: si el tipo de especie no cambia,
    // la versión nueva conserva los archivos de la vigente.
    private static Map<UUID, String> archivosHeredados(FichaTaxonomica vigente, DatosFicha datos) {
        Map<UUID, String> archivos = new HashMap<>();
        if (datos == null || !Objects.equals(datos.tipoEspecieId(), vigente.getTipoEspecie().getId())) {
            return archivos;
        }
        for (ValorAtributo valor : vigente.getValores()) {
            if (valor.getDefinicion().getTipoDato() == TipoDato.ARCHIVO) {
                archivos.put(valor.getDefinicion().getId(), valor.getValor());
            }
        }
        return archivos;
    }

    private static boolean mismaClasificacion(FichaTaxonomica a, FichaTaxonomica b) {
        return Objects.equals(a.getTipoEspecie().getId(), b.getTipoEspecie().getId())
                && Objects.equals(a.getReino(), b.getReino())
                && Objects.equals(a.getFilo(), b.getFilo())
                && Objects.equals(a.getClase(), b.getClase())
                && Objects.equals(a.getOrden(), b.getOrden())
                && Objects.equals(a.getFamilia(), b.getFamilia())
                && Objects.equals(a.getGenero(), b.getGenero())
                && Objects.equals(a.getEspecie(), b.getEspecie())
                && valores(a).equals(valores(b));
    }

    private static Map<UUID, String> valores(FichaTaxonomica ficha) {
        Map<UUID, String> valores = new HashMap<>();
        ficha.getValores().forEach(v -> valores.put(v.getDefinicion().getId(), v.getValor()));
        return valores;
    }

    private static String nombre(FichaTaxonomica ficha) {
        return ficha.getGenero() + " " + ficha.getEspecie();
    }
}
