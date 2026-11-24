package co.edu.unbosque.biohub.interna.equipos.aplicacion;

import co.edu.unbosque.biohub.interna.equipos.dominio.Responsable;

import java.util.List;

// HU-05. Si equipos viene vacía, el frontend dice que el investigador no tiene equipos asignados
public record EquiposDeInvestigador(Responsable investigador, List<EquipoRegistrado> equipos) {
}
