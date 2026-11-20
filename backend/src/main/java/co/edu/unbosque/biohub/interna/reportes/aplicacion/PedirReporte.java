package co.edu.unbosque.biohub.interna.reportes.aplicacion;

import co.edu.unbosque.biohub.interna.reportes.dominio.MesInvalidoException;
import co.edu.unbosque.biohub.interna.reportes.dominio.Reporte;
import co.edu.unbosque.biohub.interna.reportes.dominio.RepositorioReportes;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

// HU-18: el director pide el reporte de un mes. Lo calcula la analítica, no esta API (ASR-6)
@Service
public class PedirReporte {

    private final RepositorioReportes reportes;
    private final Clock reloj;

    public PedirReporte(RepositorioReportes reportes, Clock reloj) {
        this.reportes = reportes;
        this.reloj = reloj;
    }

    @Transactional
    public Reporte ejecutar(String mesTexto, UsuarioAutenticado director) {
        YearMonth mes = leerMes(mesTexto);
        // Solo meses terminados: con un mes a medias la comparación saldría engañosa
        if (!mes.isBefore(YearMonth.now(reloj))) {
            throw new MesInvalidoException("Solo se pueden pedir reportes de meses que ya terminaron.");
        }
        // Criterio 3: si ya se pidió, se devuelve el mismo y no se vuelve a calcular
        return reportes.buscarPorMes(mes).orElseGet(() -> reportes.crearPendiente(mes, director.id()));
    }

    private static YearMonth leerMes(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new MesInvalidoException("Falta el mes del reporte, con el formato AAAA-MM.");
        }
        try {
            return YearMonth.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new MesInvalidoException("El mes debe tener el formato AAAA-MM, por ejemplo 2026-10.");
        }
    }
}
