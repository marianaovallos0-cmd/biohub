package co.edu.unbosque.biohub.interna.hallazgos.dominio;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "evidencia")
public class Evidencia {

    public static final int LARGO_MAXIMO_DESCRIPCION = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hallazgo_id", nullable = false, updatable = false)
    private UUID hallazgoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15, updatable = false)
    private TipoEvidencia tipo;

    @Column(name = "url_archivo", nullable = false, updatable = false)
    private String urlArchivo;

    @Column(length = LARGO_MAXIMO_DESCRIPCION)
    private String descripcion;

    @Column(name = "fecha_carga", nullable = false, updatable = false)
    private OffsetDateTime fechaCarga;

    protected Evidencia() { }

    public Evidencia(UUID hallazgoId, TipoEvidencia tipo, String urlArchivo, String descripcion,
                     OffsetDateTime fechaCarga) {
        this.hallazgoId = hallazgoId;
        this.tipo = tipo;
        this.urlArchivo = urlArchivo;
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        this.fechaCarga = fechaCarga;
    }

    public UUID getId() { return id; }

    public UUID getHallazgoId() { return hallazgoId; }

    public TipoEvidencia getTipo() { return tipo; }

    public String getUrlArchivo() { return urlArchivo; }

    public String getDescripcion() { return descripcion; }

    public OffsetDateTime getFechaCarga() { return fechaCarga; }
}
