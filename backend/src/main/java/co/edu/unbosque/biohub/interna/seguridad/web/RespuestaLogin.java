package co.edu.unbosque.biohub.interna.seguridad.web;

import java.time.Instant;

public record RespuestaLogin(String token, Instant venceEn, String nombre, String rol) {
    
}
