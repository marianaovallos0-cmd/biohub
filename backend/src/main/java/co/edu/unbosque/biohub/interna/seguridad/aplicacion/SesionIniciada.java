package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion;

import java.time.Instant;

public record SesionIniciada(String token, Instant venceEn, String nombre, NivelAutorizacion rol) {
}