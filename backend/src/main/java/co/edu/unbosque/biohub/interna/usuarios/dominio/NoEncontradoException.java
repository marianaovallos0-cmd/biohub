package co.edu.unbosque.biohub.interna.usuarios.dominio;

/** El usuario, rol o permiso pedido no existe (404). */
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
