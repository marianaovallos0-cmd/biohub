package co.edu.unbosque.biohub.interna.equipos.aplicacion;

import co.edu.unbosque.biohub.interna.equipos.dominio.TipoEquipo;

import java.util.UUID;

// Lo que llena el administrador al registrar un equipo (HU-04)
public record NuevoEquipo(String codigoIdentificacion, TipoEquipo tipo, UUID investigadorId) {
}
