package co.edu.unbosque.biohub.publica.integracion.web;

import co.edu.unbosque.biohub.publica.integracion.aplicacion.AutenticarConsumidor;
import co.edu.unbosque.biohub.publica.integracion.dominio.ClaveInvalidaException;
import co.edu.unbosque.biohub.publica.integracion.dominio.ConsumidorExterno;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * HU-16: toda petición a /externo/** debe traer una X-API-Key válida.
 * Si falta o no existe, responde 401 y la petición no sigue.
 * Si es válida, el consumidor queda como el usuario de la petición y Spring Security
 * decide si la ruta está habilitada para externos (si no, 403).
 *
 * No es un @Component a propósito: lo crea PerfilPublicaConfig dentro de la cadena
 * de seguridad, para que no se registre dos veces.
 */
public class FiltroApiKey extends OncePerRequestFilter {

    public static final String ENCABEZADO = "X-API-Key";
    public static final String AUTORIDAD = "CONSUMIDOR_EXTERNO";

    private final AutenticarConsumidor autenticar;

    public FiltroApiKey(AutenticarConsumidor autenticar) {
        this.autenticar = autenticar;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return !peticion.getRequestURI().startsWith("/externo/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        ConsumidorExterno consumidor;
        try {
            consumidor = autenticar.ejecutar(peticion.getHeader(ENCABEZADO));
        } catch (ClaveInvalidaException e) {
            responder401(respuesta, e.getMessage());
            return;
        }

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken(
                consumidor, null, List.of(new SimpleGrantedAuthority(AUTORIDAD))));
        SecurityContextHolder.setContext(contexto);

        cadena.doFilter(peticion, respuesta);
    }

    private static void responder401(HttpServletResponse respuesta, String detalle) throws IOException {
        respuesta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        respuesta.setHeader("WWW-Authenticate", "ApiKey header=\"" + ENCABEZADO + "\"");
        respuesta.setContentType("application/problem+json");
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write(
                "{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"" + detalle + "\"}");
    }
}
