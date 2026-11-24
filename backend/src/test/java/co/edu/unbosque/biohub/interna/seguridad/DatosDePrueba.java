package co.edu.unbosque.biohub.interna.seguridad;

import co.edu.unbosque.biohub.interna.seguridad.dominio.NivelAutorizacion;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Permiso;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Rol;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/** Arma roles y usuarios para las pruebas, sin base de datos. */
public final class DatosDePrueba {

    public static final String CLAVE = "biohub123";
    // Costo 4 en vez de 10 para que las pruebas sean rápidas (la F de FIRST).
    public static final PasswordEncoder BCRYPT = new BCryptPasswordEncoder(4);

    private DatosDePrueba() { }

    public static Rol rol(NivelAutorizacion nivel, String... permisos) {
        return new Rol(nivel.name(), nivel, Arrays.stream(permisos)
                .map(codigo -> new Permiso(codigo, codigo))
                .collect(Collectors.toSet()));
    }

    public static Usuario usuario(String nombre, String correo, Rol rol) {
        Usuario usuario = new Usuario(nombre, correo, BCRYPT.encode(CLAVE), rol);
        // En la base el id lo pone PostgreSQL; aquí lo ponemos a mano.
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        return usuario;
    }
}