package co.edu.unbosque.biohub.interna.hallazgos.dominio;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

// Un hallazgo registrado por un investigador (HU-01).
@Entity
@Table(name = "hallazgo")
public class Hallazgo {

    static final int LARGO_MAXIMO_EXPEDICION = 150;

    @Id
    private UUID id;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private OffsetDateTime fechaRegistro;

    @Column(length = LARGO_MAXIMO_EXPEDICION)
    private String expedicion;

    // El autor. Se guarda el id para no amarrar Hallazgos a Seguridad
    @Column(name = "investigador_id", nullable = false, updatable = false)
    private UUID investigadorId;

    protected Hallazgo() { }

    public Hallazgo(String expedicion, UUID investigadorId, OffsetDateTime fechaRegistro) {
        if (expedicion != null && expedicion.trim().length() > LARGO_MAXIMO_EXPEDICION) {
            throw new IllegalArgumentException(errorExpedicion());
        }
        this.id = UUID.randomUUID();
        this.expedicion = expedicion == null || expedicion.isBlank() ? null : expedicion.trim();
        this.investigadorId = investigadorId;
        this.fechaRegistro = fechaRegistro;
    }

    public static boolean expedicionValida(String expedicion) {
        return expedicion == null || expedicion.trim().length() <= LARGO_MAXIMO_EXPEDICION;
    }

    public static String errorExpedicion() {
        return "El nombre de la expedición tiene más de " + LARGO_MAXIMO_EXPEDICION + " caracteres.";
    }

    public boolean esAutor(UUID usuarioId) {
        return investigadorId.equals(usuarioId);
    }

    public UUID getId() { return id; }

    public OffsetDateTime getFechaRegistro() { return fechaRegistro; }

    public String getExpedicion() { return expedicion; }

    public UUID getInvestigadorId() { return investigadorId; }
}
