package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import java.time.Instant;

/** El token y la hora en que se vence. */
public record TokenEmitido(String valor, Instant venceEn) {
}