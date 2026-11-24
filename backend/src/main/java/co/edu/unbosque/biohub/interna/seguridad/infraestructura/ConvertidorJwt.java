package co.edu.unbosque.biohub.interna.seguridad.infraestructura;

import co.edu.unbosque.biohub.interna.seguridad.aplicacion.CargarUsuarioAutenticado;
import co.edu.unbosque.biohub.interna.seguridad.aplicacion.UsuarioAutenticado;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

// Convierte un token ya validado en el usuario de la petición. Del token solo se usa el id; el rol y los permisos salen de la base

@Component
public class ConvertidorJwt implements Converter<Jwt, AbstractAuthenticationToken> {

    private final CargarUsuarioAutenticado cargarUsuario;

    public ConvertidorJwt(CargarUsuarioAutenticado cargarUsuario) {
        this.cargarUsuario = cargarUsuario;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID usuarioId;
        try {
            usuarioId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidBearerTokenException("Token inválido.");
        }

        UsuarioAutenticado usuario = cargarUsuario.ejecutar(usuarioId)
                .orElseThrow(() -> new InvalidBearerTokenException("El usuario no existe o está inactivo."));

        // Cada permiso (REGISTRAR_HALLAZGO, VER_COORDENADAS...) se vuelve una "autoridad" de Spring
        List<GrantedAuthority> autoridades = usuario.permisos().stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();

        return new UsernamePasswordAuthenticationToken(usuario, jwt, autoridades);
    }
}