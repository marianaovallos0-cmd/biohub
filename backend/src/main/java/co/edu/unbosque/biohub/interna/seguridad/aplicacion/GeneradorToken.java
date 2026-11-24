package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import java.util.UUID;

/** Crea el token de sesión. Es una interfaz para poder usar un doble en las pruebas. */
public interface GeneradorToken {

    TokenEmitido generar(UUID usuarioId);
}