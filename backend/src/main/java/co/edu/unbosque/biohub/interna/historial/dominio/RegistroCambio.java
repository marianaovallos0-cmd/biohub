package co.edu.unbosque.biohub.interna.historial.dominio;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// HU-07. Quién autorizó un cambio en la taxonomía de un hallazgo y por qué.
// Es inmutable: no tiene setters, todas las columnas son updatable = false y en la base
// el usuario de api-interna solo tiene SELECT e INSERT sobre registro_cambio (V3__permisos.sql).
@Entity
@Table(name = "registro_cambio")
public class RegistroCambio {

    public static final int LARGO_MAXIMO_JUSTIFICACION = 2000;
    public static final int LARGO_MINIMO_JUSTIFICACION = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private OffsetDateTime fechaHora;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String justificacion;

    // Se guarda el id del usuario para no amarrar Historial a Seguridad (igual que Hallazgo)
    @Column(name = "autorizado_por", nullable = false, updatable = false)
    private UUID autorizadoPor;

    @Column(name = "version_anterior_id", nullable = false, updatable = false)
    private UUID versionAnteriorId;

    @Column(name = "version_nueva_id", nullable = false, updatable = false, unique = true)
    private UUID versionNuevaId;

    protected RegistroCambio() { }

    public RegistroCambio(OffsetDateTime fechaHora, String justificacion, UUID autorizadoPor,
                          UUID versionAnteriorId, UUID versionNuevaId) {
        List<String> errores = errores(justificacion, autorizadoPor);
        if (fechaHora == null) {
            errores.add("Falta la fecha y hora del cambio.");
        }
        if (versionAnteriorId == null || versionNuevaId == null) {
            errores.add("El cambio necesita la versión anterior y la nueva.");
        } else if (versionAnteriorId.equals(versionNuevaId)) {
            errores.add("La versión nueva no puede ser la misma versión anterior.");
        }
        if (!errores.isEmpty()) {
            throw new CambioInvalidoException(errores);
        }
        this.fechaHora = fechaHora;
        this.justificacion = justificacion.trim();
        this.autorizadoPor = autorizadoPor;
        this.versionAnteriorId = versionAnteriorId;
        this.versionNuevaId = versionNuevaId;
    }

    // Criterio 1: sin justificación o sin autorizador no se guarda. Se puede revisar antes de armar el cambio.
    public static List<String> errores(String justificacion, UUID autorizadoPor) {
        List<String> errores = new ArrayList<>();
        String limpia = justificacion == null ? "" : justificacion.trim();
        if (limpia.isEmpty()) {
            errores.add("Falta la justificación del cambio.");
        } else if (limpia.length() < LARGO_MINIMO_JUSTIFICACION) {
            errores.add("La justificación debe tener al menos " + LARGO_MINIMO_JUSTIFICACION + " caracteres.");
        } else if (limpia.length() > LARGO_MAXIMO_JUSTIFICACION) {
            errores.add("La justificación tiene más de " + LARGO_MAXIMO_JUSTIFICACION + " caracteres.");
        }
        if (autorizadoPor == null) {
            errores.add("Falta el usuario que autoriza el cambio.");
        }
        return errores;
    }

    public UUID getId() { return id; }

    public OffsetDateTime getFechaHora() { return fechaHora; }

    public String getJustificacion() { return justificacion; }

    public UUID getAutorizadoPor() { return autorizadoPor; }

    public UUID getVersionAnteriorId() { return versionAnteriorId; }

    public UUID getVersionNuevaId() { return versionNuevaId; }
}
