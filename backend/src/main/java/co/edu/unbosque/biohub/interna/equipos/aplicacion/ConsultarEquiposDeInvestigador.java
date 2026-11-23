package co.edu.unbosque.biohub.interna.equipos.aplicacion;

import co.edu.unbosque.biohub.interna.equipos.dominio.DirectorioInvestigadores;
import co.edu.unbosque.biohub.interna.equipos.dominio.InvestigadorNoEncontradoException;
import co.edu.unbosque.biohub.interna.equipos.dominio.RepositorioEquipos;
import co.edu.unbosque.biohub.interna.equipos.dominio.Responsable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// HU-05. Investigador.consultarEquipos() del diagrama de clases: qué equipos tiene a cargo un investigador.
// Un investigador sin equipos devuelve la lista vacía (no es un error); uno que no existe sí lo es (404).
@Service
public class ConsultarEquiposDeInvestigador {

    private final RepositorioEquipos equipos;
    private final DirectorioInvestigadores investigadores;

    public ConsultarEquiposDeInvestigador(RepositorioEquipos equipos, DirectorioInvestigadores investigadores) {
        this.equipos = equipos;
        this.investigadores = investigadores;
    }

    @Transactional(readOnly = true)
    public EquiposDeInvestigador ejecutar(UUID investigadorId) {
        Responsable investigador = investigadores.buscarActivo(investigadorId)
                .orElseThrow(InvestigadorNoEncontradoException::new);
        return new EquiposDeInvestigador(investigador, equipos.listarPorInvestigador(investigadorId).stream()
                .map(e -> EquipoRegistrado.de(e, investigador))
                .toList());
    }
}
