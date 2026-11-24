package co.edu.unbosque.biohub.interna.taxonomia.dominio;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "tipo_especie")
public class TipoEspecie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @OneToMany(mappedBy = "tipoEspecie", cascade = CascadeType.PERSIST)
    @OrderBy("nombre ASC")
    private List<DefinicionAtributo> definiciones = new ArrayList<>();

    protected TipoEspecie() { }

    public TipoEspecie(String nombre) {
        this.nombre = nombre;
    }

    static final int LARGO_MAXIMO_NOMBRE = 100;

    // HU-03. Agregar un atributo es crear una definición nueva: no cambia el código ni las tablas (ASR-7).
    // Las fichas anteriores no se tocan; si el atributo es obligatorio, solo se exige en las fichas nuevas.
    // El nombre se guarda en minúsculas y con guiones bajos ("Color de piel" -> "color_de_piel").
    public DefinicionAtributo agregarAtributo(String nombre, TipoDato tipoDato, boolean obligatorio) {
        String limpio = normalizar(nombre);
        if (limpio.isEmpty()) {
            throw new AtributoInvalidoException("El nombre del atributo es obligatorio.");
        }
        if (limpio.length() > LARGO_MAXIMO_NOMBRE) {
            throw new AtributoInvalidoException("El nombre del atributo tiene más de " + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
        if (tipoDato == null) {
            throw new AtributoInvalidoException("Falta el tipo de dato del atributo.");
        }
        boolean repetido = definiciones.stream().anyMatch(d -> d.getNombre().equalsIgnoreCase(limpio));
        if (repetido) {
            throw new AtributoInvalidoException("El tipo «" + this.nombre + "» ya tiene el atributo «" + limpio + "».");
        }
        DefinicionAtributo definicion = new DefinicionAtributo(this, limpio, tipoDato, obligatorio);
        definiciones.add(definicion);
        return definicion;
    }

    static String normalizar(String nombre) {
        if (nombre == null) {
            return "";
        }
        return nombre.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", "_");
    }

    public Optional<DefinicionAtributo> definicion(UUID definicionId) {
        return definiciones.stream().filter(d -> d.getId() != null && d.getId().equals(definicionId)).findFirst();
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public List<DefinicionAtributo> getDefiniciones() {
        return Collections.unmodifiableList(definiciones);
    }
}
