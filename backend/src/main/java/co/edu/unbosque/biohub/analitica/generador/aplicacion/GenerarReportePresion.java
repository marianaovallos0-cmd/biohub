package co.edu.unbosque.biohub.analitica.generador.aplicacion;

import co.edu.unbosque.biohub.analitica.generador.dominio.ConteoDepartamento;
import co.edu.unbosque.biohub.analitica.generador.dominio.IndicadorDepartamento;
import co.edu.unbosque.biohub.analitica.generador.dominio.ReportePendiente;
import co.edu.unbosque.biohub.analitica.generador.dominio.RepositorioAnalitico;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

// HU-18: calcula el reporte de presión ecológica de un mes y lo deja guardado
@Service
public class GenerarReportePresion {

    private final RepositorioAnalitico repositorio;
    private final Clock reloj;

    public GenerarReportePresion(RepositorioAnalitico repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Transactional
    public List<IndicadorDepartamento> generar(ReportePendiente pendiente) {
        Map<String, ConteoDepartamento> actual = porDepartamento(repositorio.contar(pendiente.mes()));
        Map<String, ConteoDepartamento> anterior = porDepartamento(repositorio.contar(pendiente.mes().minusMonths(1)));

        // Entran los departamentos con hallazgos este mes o el anterior: si uno bajó a cero también tiene que salir
        Set<String> departamentos = new TreeSet<>(actual.keySet());
        departamentos.addAll(anterior.keySet());

        List<IndicadorDepartamento> indicadores = departamentos.stream()
                .map(d -> IndicadorDepartamento.calcular(
                        actual.getOrDefault(d, ConteoDepartamento.vacio(d)),
                        anterior.getOrDefault(d, ConteoDepartamento.vacio(d))))
                .toList();

        // Si el mes no tiene registros la lista queda vacía y el reporte se guarda igual, en cero (criterio 2)
        repositorio.guardar(pendiente.id(), indicadores, OffsetDateTime.now(reloj));
        return indicadores;
    }

    private static Map<String, ConteoDepartamento> porDepartamento(List<ConteoDepartamento> conteos) {
        return conteos.stream().collect(Collectors.toMap(ConteoDepartamento::departamento, Function.identity()));
    }
}