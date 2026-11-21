package co.edu.unbosque.biohub.interna.usuarios.dominio;

/**
 * La acción choca con el estado actual (409): un correo que ya existe, o un
 * administrador que se dejaría a sí mismo sin acceso.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
