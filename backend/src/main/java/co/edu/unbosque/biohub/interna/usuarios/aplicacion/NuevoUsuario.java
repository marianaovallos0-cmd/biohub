package co.edu.unbosque.biohub.interna.usuarios.aplicacion;

import java.util.UUID;

/** Lo que el administrador envía para crear un usuario. */
public record NuevoUsuario(String nombre, String correo, String contrasena, UUID rolId) {
}
