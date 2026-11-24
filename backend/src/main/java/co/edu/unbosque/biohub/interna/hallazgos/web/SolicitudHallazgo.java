package co.edu.unbosque.biohub.interna.hallazgos.web;

import co.edu.unbosque.biohub.interna.hallazgos.aplicacion.DatosUbicacion;
import co.edu.unbosque.biohub.interna.hallazgos.dominio.TipoEvidencia;
import co.edu.unbosque.biohub.interna.taxonomia.aplicacion.DatosFicha;

import java.util.List;

public record SolicitudHallazgo(String expedicion, DatosFicha ficha, DatosUbicacion ubicacion,
                                List<DescripcionEvidencia> evidencias) {

    public record DescripcionEvidencia(TipoEvidencia tipo, String descripcion) { }
}
