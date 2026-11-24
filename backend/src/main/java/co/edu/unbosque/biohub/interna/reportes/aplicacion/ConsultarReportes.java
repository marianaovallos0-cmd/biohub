package co.edu.unbosque.biohub.interna.reportes.aplicacion;

import co.edu.unbosque.biohub.interna.reportes.dominio.Reporte;
import co.edu.unbosque.biohub.interna.reportes.dominio.ReporteNoEncontradoException;
import co.edu.unbosque.biohub.interna.reportes.dominio.RepositorioReportes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// HU-18, criterio 3: lee lo que ya calculó la analítica, sin recalcular nada
@Service
public class ConsultarReportes {

    private final RepositorioReportes reportes;

    public ConsultarReportes(RepositorioReportes reportes) {
        this.reportes = reportes;
    }

    @Transactional(readOnly = true)
    public List<ResumenReporte> listar() {
        return reportes.listar().stream().map(ResumenReporte::de).toList();
    }

    @Transactional(readOnly = true)
    public DetalleReporte detalle(UUID id) {
        Reporte reporte = reportes.buscarPorId(id).orElseThrow(ReporteNoEncontradoException::new);
        return DetalleReporte.de(reporte, reportes.filas(id));
    }
}
