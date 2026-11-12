package co.edu.unbosque.biohub.interna.seguridad.dominio;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

//En el rol se agrupan los permisos, el usuario tiene un solo rol
@Entity 
@Table(name = "rol")
public class Rol {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_autorizacion", nullable = false, length = 20)
    private NivelAutorizacion nivelAutorizacion;

    //Este Join es para que los permiso se carguen con el rol
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "rol_permiso",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id"))
    private Set<Permiso> permisos = new HashSet<>();

    protected Rol() {}

    public Rol(String nombre, NivelAutorizacion nivelAutorizacion, Set<Permiso> permisos) {
        this.nombre = nombre;
        this.nivelAutorizacion = nivelAutorizacion;
        this.permisos = permisos;
    }

    public boolean tienePermiso(String codigo) {
       return permisos.stream().anyMatch(p -> p.getCodigo().equals(codigo));
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public NivelAutorizacion getNivelAutorizacion() {
        return nivelAutorizacion;
    }

    public Set<Permiso> getPermisos() {
        return permisos;
    }

    
}
