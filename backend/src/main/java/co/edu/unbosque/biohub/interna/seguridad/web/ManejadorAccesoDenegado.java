package co.edu.unbosque.biohub.interna.seguridad.web;

import co.edu.unbosque.biohub.interna.seguridad.aplicacion.ServicioAutorizacion;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Responde 403 y guarda el intento (HU-14)
@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorAccesoDenegado.class);

    private final ServicioAutorizacion autorizacion;

    public ManejadorAccesoDenegado(ServicioAutorizacion autorizacion) {
        this.autorizacion = autorizacion;
    }

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta,
                       AccessDeniedException e) throws IOException {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario) {
            String recurso = peticion.getMethod() + " " + peticion.getRequestURI();
            try {
                autorizacion.registrarIntento(usuario, recurso, peticion.getRemoteAddr());
            } catch (RuntimeException error) {
                // Si falla el registro, igual se responde 403
                log.error("No se pudo guardar el intento de {} a {}", usuario.correo(), recurso, error);
            }
        }

        respuesta.setStatus(HttpServletResponse.SC_FORBIDDEN);
        respuesta.setContentType("application/problem+json");
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write(
                "{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"Tu rol no tiene permiso para esta acción.\"}");
    }
}