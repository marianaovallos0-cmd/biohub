package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import org.springframework.stereotype.Service;


@Service("autorizacion")
public class ServicioAutorizacion {

    public boolean validarAcceso(UsuarioAutenticado usuario, String permiso) {
        return usuario != null && usuario.tienePermiso(permiso);
    }
}