package co.edu.unbosque.biohub.interna.taxonomia.dominio;

import jakarta.persistence.*;

import java.util.UUID;

// Qué se mide en un tipo de especie: nombre, tipo de dato y si es obligatorio
@Entity
@Table(name = "definicion_atributo")
public class DefinicionAtributo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_especie_id", nullable = false, updatable = false)
    private TipoEspecie tipoEspecie;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dato", nullable = false, length = 10)
    private TipoDato tipoDato;

    @Column(nullable = false)
    private boolean obligatorio;

    protected DefinicionAtributo() { }

    DefinicionAtributo(TipoEspecie tipoEspecie, String nombre, TipoDato tipoDato, boolean obligatorio) {
        this.tipoEspecie = tipoEspecie;
        this.nombre = nombre;
        this.tipoDato = tipoDato;
        this.obligatorio = obligatorio;
    }

    // La estrategia sale del tipo de dato: NUMERO usa ValidadorNumero, FECHA usa ValidadorFecha...
    public boolean aceptaValor(String valor) {
        return tipoDato.validador().esValido(valor);
    }

    public UUID getId() {
        return id;
    }

    public TipoEspecie getTipoEspecie() {
        return tipoEspecie;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoDato getTipoDato() {
        return tipoDato;
    }

    public boolean isObligatorio() {
        return obligatorio;
    }
}
