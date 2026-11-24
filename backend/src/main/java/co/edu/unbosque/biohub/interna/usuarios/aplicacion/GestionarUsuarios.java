package co.edu.unbosque.biohub.interna.usuarios.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.dominio.Rol;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import co.edu.unbosque.biohub.interna.usuarios.dominio.ConflictoException;
import co.edu.unbosque.biohub.interna.usuarios.dominio.DatosInvalidosException;
import co.edu.unbosque.biohub.interna.usuarios.dominio.NoEncontradoException;
import co.edu.unbosque.biohub.interna.usuarios.dominio.RepositorioGestionUsuarios;
import co.edu.unbosque.biohub.interna.usuarios.dominio.RepositorioRoles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * HU-15: el administrador crea usuarios, les cambia el rol y los activa o desactiva.
 * Un usuario no se borra, se desactiva, para que sus cambios e intentos sigan apuntando a alguien.
 */
@Service
public class GestionarUsuarios {

    public static final int LARGO_MINIMO_CONTRASENA = 8;
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final RepositorioGestionUsuarios usuarios;
    private final RepositorioRoles roles;
    private final PasswordEncoder contrasenas;

    public GestionarUsuarios(RepositorioGestionUsuarios usuarios, RepositorioRoles roles,
                             PasswordEncoder contrasenas) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.contrasenas = contrasenas;
    }

    @Transactional(readOnly = true)
    public List<UsuarioVista> listar() {
        return usuarios.listar().stream()
                .sorted(Comparator.comparing(Usuario::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(UsuarioVista::de)
                .toList();
    }

    @Transactional
    public UsuarioVista crear(NuevoUsuario datos) {
        // Tercer criterio: sin rol no se guarda. La base también lo exige (rol_id NOT NULL)
        if (datos.rolId() == null) {
            throw new DatosInvalidosException("El usuario debe tener un rol.");
        }
        String nombre = obligatorio(datos.nombre(), "El nombre es obligatorio.", 100);
        String correo = obligatorio(datos.correo(), "El correo es obligatorio.", 150).toLowerCase(Locale.ROOT);
        if (!CORREO.matcher(correo).matches()) {
            throw new DatosInvalidosException("El correo no es válido.");
        }
        if (datos.contrasena() == null || datos.contrasena().length() < LARGO_MINIMO_CONTRASENA) {
            throw new DatosInvalidosException(
                    "La contraseña debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres.");
        }
        Rol rol = roles.buscarRol(datos.rolId())
                .orElseThrow(() -> new DatosInvalidosException("El rol elegido no existe."));
        if (usuarios.existeCorreo(correo)) {
            throw new ConflictoException("Ya existe un usuario con el correo " + correo + ".");
        }

        Usuario nuevo = new Usuario(nombre, correo, contrasenas.encode(datos.contrasena()), rol);
        return UsuarioVista.de(usuarios.guardar(nuevo));
    }

    @Transactional
    public UsuarioVista cambiarRol(UUID usuarioId, UUID rolId, UUID quienLoHace) {
        if (rolId == null) {
            throw new DatosInvalidosException("El usuario debe tener un rol.");
        }
        Usuario usuario = buscar(usuarioId);
        if (usuario.getId().equals(quienLoHace)) {
            throw new ConflictoException("No puedes cambiar tu propio rol.");
        }
        Rol rol = roles.buscarRol(rolId)
                .orElseThrow(() -> new DatosInvalidosException("El rol elegido no existe."));
        usuario.cambiarRol(rol);
        return UsuarioVista.de(usuarios.guardar(usuario));
    }

    @Transactional
    public UsuarioVista activar(UUID usuarioId) {
        Usuario usuario = buscar(usuarioId);
        usuario.activar();
        return UsuarioVista.de(usuarios.guardar(usuario));
    }

    @Transactional
    public UsuarioVista desactivar(UUID usuarioId, UUID quienLoHace) {
        Usuario usuario = buscar(usuarioId);
        if (usuario.getId().equals(quienLoHace)) {
            throw new ConflictoException("No puedes desactivarte a ti mismo.");
        }
        usuario.desactivar();
        return UsuarioVista.de(usuarios.guardar(usuario));
    }

    private Usuario buscar(UUID usuarioId) {
        return usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("No existe el usuario " + usuarioId + "."));
    }

    private static String obligatorio(String valor, String mensaje, int largoMaximo) {
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException(mensaje);
        }
        String limpio = valor.trim();
        if (limpio.length() > largoMaximo) {
            throw new DatosInvalidosException("Máximo " + largoMaximo + " caracteres.");
        }
        return limpio;
    }
}
