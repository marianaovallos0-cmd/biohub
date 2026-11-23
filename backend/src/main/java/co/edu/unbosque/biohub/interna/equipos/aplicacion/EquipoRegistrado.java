package co.edu.unbosque.biohub.interna.equipos.aplicacion;

import co.edu.unbosque.biohub.interna.equipos.dominio.EquipoCampo;
import co.edu.unbosque.biohub.interna.equipos.dominio.Responsable;
import co.edu.unbosque.biohub.interna.equipos.dominio.TipoEquipo;

import java.util.UUID;

public record EquipoRegistrado(UUID id, String codigoIdentificacion, TipoEquipo tipo, String estado,
                               UUID investigadorId, String responsable) {

    public static EquipoRegistrado de(EquipoCampo equipo, String responsable) {
        return new EquipoRegistrado(equipo.getId(), equipo.getCodigoIdentificacion(), equipo.getTipo(),
                equipo.getEstado(), equipo.getInvestigadorId(), responsable);
    }

    public static EquipoRegistrado de(EquipoCampo equipo, Responsable responsable) {
        return de(equipo, responsable.nombre());
    }
}
