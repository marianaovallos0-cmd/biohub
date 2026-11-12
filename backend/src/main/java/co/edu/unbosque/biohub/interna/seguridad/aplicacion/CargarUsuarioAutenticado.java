package co.edu.unbosque.biohub.interna.seguridad.aplicacion;

import co.edu.unbosque.biohub.interna.seguridad.dominio.RepositorioUsuarios;
import co.edu.unbosque.biohub.interna.seguridad.dominio.Usuario;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;


@Service
public class CargarUsuarioAutenticado {

    private final RepositorioUsuarios usuarios;

    public CargarUsuarioAutenticado(RepositorioUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    public Optional<UsuarioAutenticado> ejecutar(UUID usuarioId) {
        return usuarios.buscarPorId(usuarioId)
                .filter(Usuario::isActivo)
                .map(UsuarioAutenticado::de);
    }
}