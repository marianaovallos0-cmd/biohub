package co.edu.unbosque.biohub.interna.historial.aplicacion;

import co.edu.unbosque.biohub.interna.historial.dominio.ConsultaHallazgos;
import co.edu.unbosque.biohub.interna.historial.dominio.ResumenHallazgo;
import org.springframework.stereotype.Service;

import java.util.List;

// HU-06, criterio 3: el auditor ve la clasificación vigente de cada hallazgo y si tiene versiones anteriores
@Service
public class ListarHallazgosAuditoria {

    private final ConsultaHallazgos consulta;

    public ListarHallazgosAuditoria(ConsultaHallazgos consulta) {
        this.consulta = consulta;
    }

    public List<ResumenHallazgo> ejecutar() {
        return consulta.listarConVersionVigente();
    }
}
