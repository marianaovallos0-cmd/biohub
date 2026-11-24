package co.edu.unbosque.biohub.interna.equipos.aplicacion;

import co.edu.unbosque.biohub.interna.equipos.dominio.DirectorioInvestigadores;
import co.edu.unbosque.biohub.interna.equipos.dominio.EquipoCampo;
import co.edu.unbosque.biohub.interna.equipos.dominio.EquipoDuplicadoException;
import co.edu.unbosque.biohub.interna.equipos.dominio.EquipoInvalidoException;
import co.edu.unbosque.biohub.interna.equipos.dominio.RepositorioEquipos;
import co.edu.unbosque.biohub.interna.equipos.dominio.Responsable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// HU-04. El administrador registra un equipo con su investigador responsable.
// 1) Sin responsable no se guarda.  2) El responsable debe ser un investigador activo.
// 3) Si el código ya existe, se rechaza (y la base lo ataja también con su UNIQUE).
@Service
public class RegistrarEquipo {

    private final RepositorioEquipos equipos;
    private final DirectorioInvestigadores investigadores;

    public RegistrarEquipo(RepositorioEquipos equipos, DirectorioInvestigadores investigadores) {
        this.equipos = equipos;
        this.investigadores = investigadores;
    }

    @Transactional
    public EquipoRegistrado ejecutar(NuevoEquipo nuevo) {
        List<String> errores = new ArrayList<>(
                EquipoCampo.errores(nuevo.codigoIdentificacion(), nuevo.tipo(), nuevo.investigadorId()));

        Responsable responsable = null;
        if (nuevo.investigadorId() != null) {
            responsable = investigadores.buscarActivo(nuevo.investigadorId()).orElse(null);
            if (responsable == null) {
                errores.add("El responsable debe ser un investigador activo.");
            }
        }
        if (!errores.isEmpty()) {
            throw new EquipoInvalidoException(errores);
        }

        String codigo = EquipoCampo.normalizarCodigo(nuevo.codigoIdentificacion());
        if (equipos.existeCodigo(codigo)) {
            throw new EquipoDuplicadoException(codigo);
        }

        EquipoCampo equipo = new EquipoCampo(codigo, nuevo.tipo(), responsable.id());
        equipos.guardar(equipo);
        return EquipoRegistrado.de(equipo, responsable);
    }
}
