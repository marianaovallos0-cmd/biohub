package co.edu.unbosque.biohub.interna.historial.dominio;

import java.util.List;

// Lectura para la lista del auditor: todos los hallazgos con su versión vigente, del más reciente al más antiguo
public interface ConsultaHallazgos {

    List<ResumenHallazgo> listarConVersionVigente();
}
