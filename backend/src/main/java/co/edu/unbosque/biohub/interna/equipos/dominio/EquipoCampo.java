package co.edu.unbosque.biohub.interna.equipos.dominio;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// HU-04. Un dron, cámara trampa, hidrófono o sensor del Estado, siempre con un investigador responsable.
// El código único y el responsable obligatorio se validan aquí y también en la base (UNIQUE y NOT NULL).
@Entity
@Table(name = "equipo_campo")
public class EquipoCampo {

    static final int LARGO_MAXIMO_CODIGO = 50;
    static final String ESTADO_INICIAL = "ACTIVO";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo_identificacion", nullable = false, unique = true, length = LARGO_MAXIMO_CODIGO)
    private String codigoIdentificacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoEquipo tipo;

    @Column(nullable = false, length = 30)
    private String estado;

    // El responsable se guarda por id para no amarrar Equipos a Seguridad (igual que Hallazgo)
    @Column(name = "investigador_id", nullable = false)
    private UUID investigadorId;

    protected EquipoCampo() { }

    public EquipoCampo(String codigoIdentificacion, TipoEquipo tipo, UUID investigadorId) {
        List<String> errores = errores(codigoIdentificacion, tipo, investigadorId);
        if (!errores.isEmpty()) {
            throw new EquipoInvalidoException(errores);
        }
        this.codigoIdentificacion = normalizarCodigo(codigoIdentificacion);
        this.tipo = tipo;
        this.investigadorId = investigadorId;
        this.estado = ESTADO_INICIAL;
    }

    // "  drn-001 " y "DRN-001" son el mismo equipo
    public static String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
    }

    // Todo lo que impide crear el equipo, en una sola lista
    public static List<String> errores(String codigoIdentificacion, TipoEquipo tipo, UUID investigadorId) {
        List<String> errores = new ArrayList<>();
        String codigo = normalizarCodigo(codigoIdentificacion);
        if (codigo.isEmpty()) {
            errores.add("Falta el código de identificación del equipo.");
        } else if (codigo.length() > LARGO_MAXIMO_CODIGO) {
            errores.add("El código de identificación tiene más de " + LARGO_MAXIMO_CODIGO + " caracteres.");
        }
        if (tipo == null) {
            errores.add("Falta el tipo de equipo.");
        }
        if (investigadorId == null) {
            errores.add("Falta el investigador responsable.");
        }
        return errores;
    }

    public UUID getId() { return id; }

    public String getCodigoIdentificacion() { return codigoIdentificacion; }

    public TipoEquipo getTipo() { return tipo; }

    public String getEstado() { return estado; }

    public UUID getInvestigadorId() { return investigadorId; }
}
