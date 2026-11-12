package co.edu.unbosque.biohub.interna.seguridad.dominio;

import jakarta.persistence.*;
import java.util.UUID;

@Entity 
@Table(name = "usuario")
public class Usuario {

    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "contrasena_hash", nullable = false, length = 100)
    private String contrasenaHash;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(optional = false, fetch =FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    protected Usuario() {}

    public Usuario(String nombre, String correo, String contrasenaHash, Rol rol) {
        this.nombre = nombre;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
    }

    public boolean tienePermiso(String codigo) {
        return activo && rol.tienePermiso(codigo);
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public boolean isActivo() {
        return activo;
    }

    public Rol getRol() {
        return rol;
    }
    
    
}
