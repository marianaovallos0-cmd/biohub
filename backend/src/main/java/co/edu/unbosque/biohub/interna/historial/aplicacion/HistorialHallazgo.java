package co.edu.unbosque.biohub.interna.historial.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// HU-08: un hallazgo con todas sus versiones, de la 1 a la vigente
public record HistorialHallazgo(UUID hallazgoId, OffsetDateTime fechaRegistro, String expedicion,
                                String investigador, int versionVigente, List<Version> versiones) {

    // Una versión tal como estaba en ese momento, con sus propios atributos
    public record Version(UUID id, int numeroVersion, boolean vigente, OffsetDateTime fechaVersion,
                          String tipoEspecie, String reino, String filo, String clase, String orden,
                          String familia, String genero, String especie,
                          List<Atributo> atributos, Cambio cambio) {
    }

    public record Atributo(String nombre, String tipoDato, String valor) {
    }

    // HU-07: quién autorizó la versión y por qué. La versión 1 no tiene cambio (es el registro original).
    public record Cambio(OffsetDateTime fechaHora, String justificacion, String autorizadoPor,
                         int versionAnterior) {
    }
}
