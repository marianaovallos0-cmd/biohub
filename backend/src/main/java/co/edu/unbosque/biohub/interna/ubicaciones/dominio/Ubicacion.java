package co.edu.unbosque.biohub.interna.ubicaciones.dominio;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ubicacion")
public class Ubicacion {

    static final int LARGO_MAXIMO = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hallazgo_id", nullable = false, unique = true, updatable = false)
    private UUID hallazgoId;

    @Column(nullable = false)
    private double latitud;

    @Column(nullable = false)
    private double longitud;

    @Column(nullable = false, length = LARGO_MAXIMO)
    private String region;

    @Column(nullable = false, length = LARGO_MAXIMO)
    private String departamento;

    @Column(name = "especie_protegida", nullable = false)
    private boolean especieProtegida;

    protected Ubicacion() { }

    public Ubicacion(UUID hallazgoId, Double latitud, Double longitud, String region, String departamento,
                     boolean especieProtegida) {
        List<String> errores = errores(latitud, longitud, region, departamento);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", errores));
        }
        this.hallazgoId = hallazgoId;
        this.latitud = latitud;
        this.longitud = longitud;
        this.region = region.trim();
        this.departamento = departamento.trim();
        this.especieProtegida = especieProtegida;
    }

    /** Lo que impide guardar la ubicación. Vacío si todo está bien. */
    public static List<String> errores(Double latitud, Double longitud, String region, String departamento) {
        List<String> errores = new ArrayList<>();
        if (latitud == null || latitud.isNaN() || latitud < -90 || latitud > 90) {
            errores.add("La latitud debe estar entre -90 y 90.");
        }
        if (longitud == null || longitud.isNaN() || longitud < -180 || longitud > 180) {
            errores.add("La longitud debe estar entre -180 y 180.");
        }
        if (region == null || region.isBlank() || region.trim().length() > LARGO_MAXIMO) {
            errores.add("Falta la región.");
        }
        if (departamento == null || departamento.isBlank() || departamento.trim().length() > LARGO_MAXIMO) {
            errores.add("Falta el departamento.");
        }
        return errores;
    }

    public UUID getId() { return id; }

    public UUID getHallazgoId() { return hallazgoId; }

    public double getLatitud() { return latitud; }

    public double getLongitud() { return longitud; }

    public String getRegion() { return region; }

    public String getDepartamento() { return departamento; }

    public boolean isEspecieProtegida() { return especieProtegida; }
}
