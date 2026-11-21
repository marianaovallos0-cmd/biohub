package co.edu.unbosque.biohub.interna.hallazgos.aplicacion;

import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.DatosFicha;

// HU-06: la clasificación nueva y por qué se cambia (HU-07)
public record SolicitudReclasificacion(DatosFicha ficha, String justificacion) {
}
