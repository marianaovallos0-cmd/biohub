package co.edu.unbosque.biohub.interna.taxonomia.aplicacion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record DatosFicha(UUID tipoEspecieId, String reino, String filo, String clase, String orden,
                         String familia, String genero, String especie, Map<UUID, String> valores) {

    public DatosFicha {
        // Los campos que llegan vacíos (null) se descartan, si eran obligatorios, la ficha lo avisa
        Map<UUID, String> limpios = new HashMap<>();
        if (valores != null) {
            valores.forEach((id, valor) -> {
                if (id != null && valor != null) {
                    limpios.put(id, valor);
                }
            });
        }
        valores = Map.copyOf(limpios);
    }

    public DatosFicha conValor(UUID definicionId, String valor) {
        Map<UUID, String> nuevos = new HashMap<>(valores);
        nuevos.put(definicionId, valor);
        return new DatosFicha(tipoEspecieId, reino, filo, clase, orden, familia, genero, especie, nuevos);
    }
}
