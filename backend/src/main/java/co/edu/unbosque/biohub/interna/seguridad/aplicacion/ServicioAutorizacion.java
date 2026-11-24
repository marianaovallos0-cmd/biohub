package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.dominio.IntentoAccesoNoAutorizado;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioIntentos;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioUsuarios;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service("autorizacion")
public class ServicioAutorizacion {

    private static final int LARGO_MAXIMO_RECURSO = 255;

    private final RepositorioUsuarios usuarios;
    private final RepositorioIntentos intentos;
    private final Clock reloj;

    public ServicioAutorizacion(RepositorioUsuarios usuarios, RepositorioIntentos intentos, Clock reloj) {
        this.usuarios = usuarios;
        this.intentos = intentos;
        this.reloj = reloj;
    }

    public boolean validarAcceso(UsuarioAutenticado usuario, String permiso) {
        return usuario != null && usuario.tienePermiso(permiso);
    }

    // Transacción propia: el intento queda guardado aunque la petición se rechace
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIntento(UsuarioAutenticado usuario, String recurso, String ip) {
        if (usuario == null) {
            return;
        }
        usuarios.buscarPorId(usuario.id()).ifPresent(u -> intentos.guardar(
                new IntentoAccesoNoAutorizado(u, recortar(recurso), ip, OffsetDateTime.now(reloj))));
    }

    private static String recortar(String recurso) {
        return recurso.length() <= LARGO_MAXIMO_RECURSO ? recurso : recurso.substring(0, LARGO_MAXIMO_RECURSO);
    }
}