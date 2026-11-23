package co.edu.unbosque.biohub.interna.equipos.dominio;

import java.util.UUID;

// Un investigador que puede quedar como responsable de un equipo
public record Responsable(UUID id, String nombre, String correo) {
}
