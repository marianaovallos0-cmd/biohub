package co.edu.unbosque.biohub.interna.seguridad.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta,
                       AccessDeniedException e) throws IOException {
        respuesta.setStatus(HttpServletResponse.SC_FORBIDDEN);
        respuesta.setContentType("application/problem+json");
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write(
                "{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"Tu rol no tiene permiso para esta acción.\"}");
    }
}