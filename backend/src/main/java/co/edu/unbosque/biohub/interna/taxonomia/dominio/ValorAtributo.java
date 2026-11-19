package co.edu.unbosque.biohub.interna.taxonomia.dominio;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "valor_atributo")
public class ValorAtributo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ficha_id", nullable = false, updatable = false)
    private FichaTaxonomica ficha;

    @ManyToOne(optional = false)
    @JoinColumn(name = "definicion_atributo_id", nullable = false, updatable = false)
    private DefinicionAtributo definicion;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String valor;

    protected ValorAtributo() { }

    ValorAtributo(FichaTaxonomica ficha, DefinicionAtributo definicion, String valor) {
        this.ficha = ficha;
        this.definicion = definicion;
        this.valor = valor;
    }

    public boolean esValido() {
        return definicion.aceptaValor(valor);
    }

    public UUID getId() {
        return id;
    }

    public FichaTaxonomica getFicha() {
        return ficha;
    }

    public DefinicionAtributo getDefinicion() {
        return definicion;
    }

    public String getValor() {
        return valor;
    }
}
