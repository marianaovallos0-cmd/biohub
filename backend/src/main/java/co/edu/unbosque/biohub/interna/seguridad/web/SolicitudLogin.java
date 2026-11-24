package co.edu.unbosque.biohub.interna.seguridad.web;

import jakarta.validation.constraints.NotBlank;

public record SolicitudLogin(@NotBlank String correo, @NotBlank String contrasena) {
}