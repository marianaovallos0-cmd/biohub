package co.edu.unbosque.biohub.interna.seguridad.web;

import co.edu.unbosque.biohub.interna.seguridad.aplicacion.IniciarSesion;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.SesionIniciada;
import co.edu.unbosque.biohub.interna.seguridad.dominio.CredencialesInvalidasException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/interno/auth")
public class AuthController {

    private final IniciarSesion iniciarSesion;

    public AuthController(IniciarSesion iniciarSesion) {
        this.iniciarSesion = iniciarSesion;
    }

    @GetMapping("/yo")
    public UsuarioAutenticado yo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuario;
    }

    @PostMapping("/login")
    public RespuestaLogin login(@Valid @RequestBody SolicitudLogin solicitud) {
        SesionIniciada sesion = iniciarSesion.ejecutar(solicitud.correo(), solicitud.contrasena());
        return new RespuestaLogin(sesion.token(), sesion.venceEn(), sesion.nombre(), sesion.rol().name());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ProblemDetail credencialesInvalidas(CredencialesInvalidasException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }
}