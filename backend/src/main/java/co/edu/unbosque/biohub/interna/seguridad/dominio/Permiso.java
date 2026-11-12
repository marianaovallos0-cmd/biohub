package co.edu.unbosque.biohub.interna.seguridad.dominio;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "permiso")
public class Permiso {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo; 

    private String descripcion;

    protected Permiso() {}

    public Permiso(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    

}
