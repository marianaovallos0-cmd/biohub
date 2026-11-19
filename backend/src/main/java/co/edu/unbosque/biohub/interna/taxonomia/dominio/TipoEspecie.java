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

    // Lo usa HU-03. Dos atributos del mismo tipo no pueden llamarse igual (UNIQUE en la base).
    public DefinicionAtributo agregarAtributo(String nombre, TipoDato tipoDato, boolean obligatorio) {
        boolean repetido = definiciones.stream().anyMatch(d -> d.getNombre().equalsIgnoreCase(nombre));
        if (repetido) {
            throw new IllegalArgumentException("El tipo «" + this.nombre + "» ya tiene el atributo «" + nombre + "».");
        }
        DefinicionAtributo definicion = new DefinicionAtributo(this, nombre, tipoDato, obligatorio);
        definiciones.add(definicion);
        return definicion;
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
