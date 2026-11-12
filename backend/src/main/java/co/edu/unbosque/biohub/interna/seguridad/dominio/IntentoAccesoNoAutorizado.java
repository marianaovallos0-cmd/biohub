package co.edu.unbosque.biohub.interna.seguridad.dominio;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

// Inmutable: no tiene setters y la base solo deja insertar (V3__permisos.sql)
@Entity
@Table(name = "intento_acceso_no_autorizado")
public class IntentoAccesoNoAutorizado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private OffsetDateTime fechaHora;

    @Column(name = "recurso_solicitado", nullable = false, updatable = false)
    private String recursoSolicitado;

    @Column(name = "ip_origen", length = 45, updatable = false)
    private String ipOrigen;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    protected IntentoAccesoNoAutorizado() { }

    public IntentoAccesoNoAutorizado(Usuario usuario, String recursoSolicitado, String ipOrigen,
                                     OffsetDateTime fechaHora) {
        this.usuario = usuario;
        this.recursoSolicitado = recursoSolicitado;
        this.ipOrigen = ipOrigen;
        this.fechaHora = fechaHora;
    }
    
    public UUID getId() {
        return id;
    }

    public OffsetDateTime getFechaHora() {
        return fechaHora;
    }

    public String getRecursoSolicitado() {
        return recursoSolicitado;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    
    
}
