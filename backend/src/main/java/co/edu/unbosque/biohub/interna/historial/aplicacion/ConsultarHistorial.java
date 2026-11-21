package co.edu.unbosque.biohub.interna.historial.aplicacion;

import co.edu.unbosque.biohub.interna.hallazgos.dominio.Hallazgo;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.HallazgoNoEncontradoException;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioHallazgos;
import co.edu.unbosque.biohub.interna.historial.dominio.RegistroCambio;
import co.edu.unbosque.biohub.interna.historial.dominio.RepositorioCambios;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioUsuarios;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.ServicioFichas;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaTaxonomica;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// HU-08. Todas las versiones de un hallazgo en orden, cada una con sus valores y su cambio (HU-07).
// Las versiones salen en una consulta por el índice único (hallazgo_id, numero_version): menos de 3 s (RNF12).
@Service
public class ConsultarHistorial {

    private final RepositorioHallazgos hallazgos;
    private final ServicioFichas fichas;
    private final RepositorioCambios cambios;
    private final RepositorioUsuarios usuarios;

    public ConsultarHistorial(RepositorioHallazgos hallazgos, ServicioFichas fichas,
                              RepositorioCambios cambios, RepositorioUsuarios usuarios) {
        this.hallazgos = hallazgos;
        this.fichas = fichas;
        this.cambios = cambios;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public HistorialHallazgo ejecutar(UUID hallazgoId) {
        Hallazgo hallazgo = hallazgos.buscarPorId(hallazgoId).orElseThrow(HallazgoNoEncontradoException::new);
        List<FichaTaxonomica> versiones = fichas.versiones(hallazgoId).stream()
                .sorted(Comparator.comparingInt(FichaTaxonomica::getNumeroVersion))
                .toList();
        if (versiones.isEmpty()) {
            throw new HallazgoNoEncontradoException();
        }

        Map<UUID, FichaTaxonomica> porId = versiones.stream()
                .collect(Collectors.toMap(FichaTaxonomica::getId, Function.identity()));
        Map<UUID, RegistroCambio> cambioPorVersionNueva = cambios.listarPorHallazgo(hallazgoId).stream()
                .collect(Collectors.toMap(RegistroCambio::getVersionNuevaId, Function.identity()));
        Map<UUID, String> nombres = new HashMap<>();

        int vigente = versiones.getLast().getNumeroVersion();
        List<HistorialHallazgo.Version> resultado = versiones.stream()
                .map(f -> aVersion(f, f.getNumeroVersion() == vigente,
                        cambioPorVersionNueva.get(f.getId()), porId, nombres))
                .toList();

        return new HistorialHallazgo(hallazgo.getId(), hallazgo.getFechaRegistro(), hallazgo.getExpedicion(),
                nombre(hallazgo.getInvestigadorId(), nombres), vigente, resultado);
    }

    private HistorialHallazgo.Version aVersion(FichaTaxonomica ficha, boolean vigente, RegistroCambio cambio,
                                               Map<UUID, FichaTaxonomica> porId, Map<UUID, String> nombres) {
        List<HistorialHallazgo.Atributo> atributos = ficha.getValores().stream()
                .map(v -> new HistorialHallazgo.Atributo(v.getDefinicion().getNombre(),
                        v.getDefinicion().getTipoDato().name(), v.getValor()))
                .sorted(Comparator.comparing(HistorialHallazgo.Atributo::nombre))
                .toList();
        HistorialHallazgo.Cambio datosCambio = null;
        if (cambio != null) {
            FichaTaxonomica anterior = porId.get(cambio.getVersionAnteriorId());
            datosCambio = new HistorialHallazgo.Cambio(cambio.getFechaHora(), cambio.getJustificacion(),
                    nombre(cambio.getAutorizadoPor(), nombres),
                    anterior == null ? ficha.getNumeroVersion() - 1 : anterior.getNumeroVersion());
        }
        return new HistorialHallazgo.Version(ficha.getId(), ficha.getNumeroVersion(), vigente,
                ficha.getFechaVersion(), ficha.getTipoEspecie().getNombre(), ficha.getReino(), ficha.getFilo(),
                ficha.getClase(), ficha.getOrden(), ficha.getFamilia(), ficha.getGenero(), ficha.getEspecie(),
                atributos, datosCambio);
    }

    private String nombre(UUID usuarioId, Map<UUID, String> cache) {
        return cache.computeIfAbsent(usuarioId, id -> usuarios.buscarPorId(id)
                .map(Usuario::getNombre)
                .orElse("Usuario desconocido"));
    }
}
