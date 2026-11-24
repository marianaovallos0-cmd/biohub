package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

// Double y no double: así se distingue "no la escribió" de 0
public record DatosUbicacion(Double latitud, Double longitud, String region, String departamento,
                             boolean especieProtegida) {
}
