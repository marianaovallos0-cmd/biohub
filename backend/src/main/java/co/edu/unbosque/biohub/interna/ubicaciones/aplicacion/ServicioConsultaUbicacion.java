package co.edu.unbosque.biohub.interna.ubicaciones.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.aplicacion.ServicioAutorizacion;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.RepositorioConsultaUbicaciones;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.UbicacionConsultada;
import co.edu.unbosque.biohub.interna.ubicaciones.dominio.UbicacionNoEncontradaException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * HU-13: ServicioConsulta.obtenerUbicacion del diagrama de clases.
 * Siempre valida el permiso ANTES de leer (100 % de las solicitudes).
 * Solo quien tiene VER_COORDENADAS (alto rango) recibe latitud y longitud.
 * A los demás se les responde región y departamento, sin nada aproximado, y su intento queda registrado (HU-14).
 */
@Service
public class ServicioConsultaUbicacion {

    public static final String PERMISO = "VER_COORDENADAS";

    private final ServicioAutorizacion autorizacion;
    private final RepositorioConsultaUbicaciones ubicaciones;

    public ServicioConsultaUbicacion(ServicioAutorizacion autorizacion, RepositorioConsultaUbicaciones ubicaciones) {
        this.autorizacion = autorizacion;
        this.ubicaciones = ubicaciones;
    }

    public UbicacionConsultada obtenerUbicacion(UUID hallazgoId, UsuarioAutenticado usuario, String recurso, String ip) {
        if (autorizacion.validarAcceso(usuario, PERMISO)) {
            return ubicaciones.exacta(hallazgoId).orElseThrow(() -> new UbicacionNoEncontradaException(hallazgoId));
        }
        autorizacion.registrarIntento(usuario, recurso, ip);
        return ubicaciones.publica(hallazgoId).orElseThrow(() -> new UbicacionNoEncontradaException(hallazgoId));
    }

    public List<UbicacionConsultada> listar(UsuarioAutenticado usuario, String recurso, String ip) {
        if (autorizacion.validarAcceso(usuario, PERMISO)) {
            return ubicaciones.todasExactas();
        }
        autorizacion.registrarIntento(usuario, recurso, ip);
        return ubicaciones.todasPublicas();
    }
}
