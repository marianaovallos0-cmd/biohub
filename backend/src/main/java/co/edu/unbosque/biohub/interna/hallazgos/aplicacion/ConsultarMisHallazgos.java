package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import co.edu.unbosque.biohub.interna.hallazgos.dominio.Evidencia;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.Hallazgo;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioEvidencias;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.RepositorioHallazgos;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.ServicioFichas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// HU-01, criterio 2: el investigador vuelve a ver sus hallazgos y las evidencias que adjuntó
@Service
public class ConsultarMisHallazgos {

    private final RepositorioHallazgos hallazgos;
    private final RepositorioEvidencias evidencias;
    private final ServicioFichas fichas;

    public ConsultarMisHallazgos(RepositorioHallazgos hallazgos, RepositorioEvidencias evidencias, ServicioFichas fichas) {
        this.hallazgos = hallazgos;
        this.evidencias = evidencias;
        this.fichas = fichas;
    }

    @Transactional(readOnly = true)
    public List<MiHallazgo> ejecutar(UsuarioAutenticado usuario) {
        return hallazgos.listarPorInvestigador(usuario.id()).stream().map(this::aMiHallazgo).toList();
    }

    private MiHallazgo aMiHallazgo(Hallazgo hallazgo) {
        // Se muestra la clasificación vigente: si el hallazgo se reclasificó (HU-06), sale la última versión
        String especie = fichas.vigente(hallazgo.getId())
                .map(f -> f.getGenero() + " " + f.getEspecie())
                .orElse("Sin clasificar");
        List<EvidenciaRegistrada> adjuntas = evidencias.listarPorHallazgo(hallazgo.getId()).stream()
                .map(e -> EvidenciaRegistrada.de(e, nombreArchivo(e)))
                .toList();
        return new MiHallazgo(hallazgo.getId(), hallazgo.getFechaRegistro(), hallazgo.getExpedicion(), especie, adjuntas);
    }

    // El nombre original no se guarda en la base; se muestra el del archivo en el disco
    private static String nombreArchivo(Evidencia evidencia) {
        String ruta = evidencia.getUrlArchivo();
        return ruta.substring(ruta.lastIndexOf('/') + 1);
    }
}
