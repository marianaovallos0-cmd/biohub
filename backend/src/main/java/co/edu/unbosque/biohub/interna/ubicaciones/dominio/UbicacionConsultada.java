package co.edu.unbosque.biohub.interna.ubicaciones.dominio;

import java.util.UUID;

/**
 * HU-13: la ubicación de un hallazgo tal como sale de la api-interna.
 * latitud y longitud solo vienen llenas para quien tiene VER_COORDENADAS; para los demás
 * son null (nunca redondeadas ni aproximadas). La capa web decide qué forma de JSON usar.
 */
public record UbicacionConsultada(
        UUID hallazgoId,
        String nombreCientifico,
        String region,
        String departamento,
        boolean especieProtegida,
        boolean exacta,
        Double latitud,
        Double longitud) {

    public static UbicacionConsultada exacta(UUID hallazgoId, String nombre, String region, String departamento,
                                             boolean protegida, double latitud, double longitud) {
        return new UbicacionConsultada(hallazgoId, nombre, region, departamento, protegida, true, latitud, longitud);
    }

    public static UbicacionConsultada publica(UUID hallazgoId, String nombre, String region, String departamento,
                                              boolean protegida) {
        return new UbicacionConsultada(hallazgoId, nombre, region, departamento, protegida, false, null, null);
    }
}
