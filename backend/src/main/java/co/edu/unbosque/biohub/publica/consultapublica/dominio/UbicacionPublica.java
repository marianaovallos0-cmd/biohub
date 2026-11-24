package co.edu.unbosque.biohub.publica.consultapublica.dominio;

import java.io.Serializable;

/**
 * Lo único de la ubicación que puede ver el público: región y departamento.
 * No tiene latitud ni longitud, así que no hay forma de que salgan por error (ASR-1).
 */
public record UbicacionPublica(String region, String departamento) implements Serializable {
}
