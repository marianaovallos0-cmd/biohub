package co.edu.unbosque.biohub.analitica.generador.dominio;

import java.time.YearMonth;

/**
 * HU-19: deja pendiente el reporte de un mes para que lo calcule el planificador.
 * Va aparte de RepositorioAnalitico para no obligar a sus dobles a implementarlo (principio I de SOLID).
 */
public interface ProgramadorReportes {

    // true si lo creó; false si ese mes ya estaba pedido (un solo reporte por tipo y mes, V4)
    boolean programar(YearMonth mes);
}
