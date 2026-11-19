package co.edu.unbosque.biohub.interna.taxonomia.dominio;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Una versión de la clasificación de un hallazgo (HU-02).
@Entity
@Table(name = "ficha_taxonomica")
public class FichaTaxonomica {

    static final int LARGO_MAXIMO_RANGO = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hallazgo_id", nullable = false, updatable = false)
    private UUID hallazgoId;

    @Column(name = "numero_version", nullable = false, updatable = false)
    private int numeroVersion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tipo_especie_id", nullable = false, updatable = false)
    private TipoEspecie tipoEspecie;

    @Column(length = LARGO_MAXIMO_RANGO, updatable = false)
    private String reino;

    @Column(length = LARGO_MAXIMO_RANGO, updatable = false)
    private String filo;

    @Column(length = LARGO_MAXIMO_RANGO, updatable = false)
    private String clase;

    @Column(length = LARGO_MAXIMO_RANGO, updatable = false)
    private String orden;

    @Column(length = LARGO_MAXIMO_RANGO, updatable = false)
    private String familia;

    @Column(length = LARGO_MAXIMO_RANGO, updatable = false)
    private String genero;

    @Column(length = LARGO_MAXIMO_RANGO, updatable = false)
    private String especie;

    @Column(name = "fecha_version", nullable = false, updatable = false)
    private OffsetDateTime fechaVersion;

    @OneToMany(mappedBy = "ficha", cascade = CascadeType.PERSIST)
    private List<ValorAtributo> valores = new ArrayList<>();

    protected FichaTaxonomica() { }

    private FichaTaxonomica(Builder b) {
        this.hallazgoId = b.hallazgoId;
        this.numeroVersion = b.numeroVersion;
        this.tipoEspecie = b.tipoEspecie;
        this.reino = b.reino;
        this.filo = b.filo;
        this.clase = b.clase;
        this.orden = b.orden;
        this.familia = b.familia;
        this.genero = b.genero;
        this.especie = b.especie;
        this.fechaVersion = b.fechaVersion;
        b.valores.forEach((definicion, valor) -> valores.add(new ValorAtributo(this, definicion, valor)));
    }

    public static Builder builder(TipoEspecie tipoEspecie) {
        return new Builder(tipoEspecie);
    }

    // true si están todos los atributos obligatorios de su tipo de especie.
    public boolean validarObligatorios() {
        return obligatoriosFaltantes().isEmpty();
    }

    private List<DefinicionAtributo> obligatoriosFaltantes() {
        return tipoEspecie.getDefiniciones().stream()
                .filter(DefinicionAtributo::isObligatorio)
                .filter(d -> valores.stream().noneMatch(v -> v.getDefinicion() == d))
                .toList();
    }

    List<String> errores() {
        List<String> errores = new ArrayList<>();
        if (genero == null) {
            errores.add("Falta el género.");
        }
        if (especie == null) {
            errores.add("Falta la especie.");
        }
        obligatoriosFaltantes().forEach(d ->
                errores.add("Falta el atributo obligatorio «" + d.getNombre() + "»."));
        valores.stream().filter(v -> !v.esValido()).forEach(v ->
                errores.add("El atributo «" + v.getDefinicion().getNombre() + "» "
                        + v.getDefinicion().getTipoDato().validador().mensajeError() + "."));
        return errores;
    }

    public UUID getId() { return id; }

    public UUID getHallazgoId() { return hallazgoId; }

    public int getNumeroVersion() { return numeroVersion; }

    public TipoEspecie getTipoEspecie() { return tipoEspecie; }

    public String getReino() { return reino; }

    public String getFilo() { return filo; }

    public String getClase() { return clase; }

    public String getOrden() { return orden; }

    public String getFamilia() { return familia; }

    public String getGenero() { return genero; }

    public String getEspecie() { return especie; }

    public OffsetDateTime getFechaVersion() { return fechaVersion; }

    public List<ValorAtributo> getValores() { return Collections.unmodifiableList(valores); }

    // Patrón Builder (HU-02, ASR-7). Arma la ficha por pasos y build() solo la entrega si es válida.
    public static final class Builder {

        private final TipoEspecie tipoEspecie;
        private final Map<DefinicionAtributo, String> valores = new LinkedHashMap<>();
        private final List<String> errores = new ArrayList<>();
        private UUID hallazgoId;
        private int numeroVersion = 1;
        private OffsetDateTime fechaVersion;
        private String reino;
        private String filo;
        private String clase;
        private String orden;
        private String familia;
        private String genero;
        private String especie;

        private Builder(TipoEspecie tipoEspecie) {
            this.tipoEspecie = Objects.requireNonNull(tipoEspecie, "El tipo de especie es obligatorio.");
        }

        public Builder paraHallazgo(UUID hallazgoId) {
            this.hallazgoId = hallazgoId;
            return this;
        }

        public Builder version(int numeroVersion) {
            if (numeroVersion < 1) {
                throw new IllegalArgumentException("La versión empieza en 1.");
            }
            this.numeroVersion = numeroVersion;
            return this;
        }

        public Builder fechaVersion(OffsetDateTime fechaVersion) {
            this.fechaVersion = fechaVersion;
            return this;
        }

        public Builder reino(String reino) {
            this.reino = rango("reino", reino);
            return this;
        }

        public Builder filo(String filo) {
            this.filo = rango("filo", filo);
            return this;
        }

        public Builder clase(String clase) {
            this.clase = rango("clase", clase);
            return this;
        }

        public Builder orden(String orden) {
            this.orden = rango("orden", orden);
            return this;
        }

        public Builder familia(String familia) {
            this.familia = rango("familia", familia);
            return this;
        }

        public Builder genero(String genero) {
            this.genero = rango("género", genero);
            return this;
        }

        public Builder especie(String especie) {
            this.especie = rango("especie", especie);
            return this;
        }

        public Builder atributo(UUID definicionId, String valor) {
            DefinicionAtributo definicion = tipoEspecie.definicion(definicionId).orElse(null);
            if (definicion == null) {
                errores.add("El atributo " + definicionId + " no pertenece al tipo «" + tipoEspecie.getNombre() + "».");
            } else if (valor != null && !valor.isBlank()) {
                valores.put(definicion, valor.trim());
            }
            return this;
        }

        public FichaTaxonomica build() {
            if (hallazgoId == null || fechaVersion == null) {
                throw new IllegalStateException("La ficha necesita el hallazgo y la fecha de la versión.");
            }
            FichaTaxonomica ficha = new FichaTaxonomica(this);
            List<String> todos = new ArrayList<>(errores);
            todos.addAll(ficha.errores());
            if (!todos.isEmpty()) {
                throw new FichaInvalidaException(todos);
            }
            return ficha;
        }

        private String rango(String nombre, String valor) {
            if (valor == null || valor.isBlank()) {
                return null;
            }
            String limpio = valor.trim();
            if (limpio.length() > LARGO_MAXIMO_RANGO) {
                errores.add("El campo «" + nombre + "» tiene más de " + LARGO_MAXIMO_RANGO + " caracteres.");
                return null;
            }
            return limpio;
        }
    }
}
