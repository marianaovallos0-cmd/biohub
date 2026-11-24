package co.edu.unbosque.biohub.interna.usuarios.web;

import java.util.UUID;

public record SolicitudUsuario(String nombre, String correo, String contrasena, UUID rolId) {
}
