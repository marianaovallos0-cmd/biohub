package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.dominio.CredencialesInvalidasException;
import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioUsuarios;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

//HU-12: el usuario entra con correo y contraseña y recibe un token. El token solo lleva su id. El rol se lee de la base en cada petición.
 
@Service 
public class IniciarSesion {
    
    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder contrasenas;
    private final GeneradorToken generadorToken;
    
    public IniciarSesion(RepositorioUsuarios usuarios, PasswordEncoder contrasenas, GeneradorToken generadorToken) {
        this.usuarios = usuarios;
        this.contrasenas = contrasenas;
        this.generadorToken = generadorToken;
    }

    public SesionIniciada ejecutar(String correo, String contrasena) {
        if (correo == null || contrasena == null) {
            throw new CredencialesInvalidasException();
        }
        Usuario usuario = usuarios.buscarPorCorreo(correo)
                .filter(Usuario::isActivo)
                .filter(u -> contrasenas.matches(contrasena, u.getContrasenaHash()))
                .orElseThrow(CredencialesInvalidasException::new);

        TokenEmitido token = generadorToken.generar(usuario.getId());
        return new SesionIniciada(token.valor(), token.venceEn(),
                usuario.getNombre(), usuario.getRol().getNivelAutorizacion());        
    }

}
