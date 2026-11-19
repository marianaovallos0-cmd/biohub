package co.edu.unbosque.biohub.interna.taxonomia.aplicacion;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaInvalidaException;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.FichaTaxonomica;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.RepositorioFichas;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.RepositorioTaxonomia;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoDato;
import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoEspecie;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ServicioFichas {

    private final RepositorioTaxonomia taxonomia;
    private final RepositorioFichas fichas;
    private final Clock reloj;

    public ServicioFichas(RepositorioTaxonomia taxonomia, RepositorioFichas fichas, Clock reloj) {
        this.taxonomia = taxonomia;
        this.fichas = fichas;
        this.reloj = reloj;
    }

    public FichaTaxonomica armar(UUID hallazgoId, int version, DatosFicha datos) {
        return armar(hallazgoId, version, datos, Map.of());
    }

    // Un atributo ARCHIVO solo toma la ruta de un archivo que de verdad se subió (archivos).
    // Si en los datos viene una ruta escrita a mano, se ignora: podría apuntar al archivo de otro hallazgo.
    public FichaTaxonomica armar(UUID hallazgoId, int version, DatosFicha datos, Map<UUID, String> archivos) {
        if (datos == null || datos.tipoEspecieId() == null) {
            throw new FichaInvalidaException(List.of("Falta el tipo de especie."));
        }
        TipoEspecie tipo = taxonomia.buscarTipo(datos.tipoEspecieId())
                .orElseThrow(() -> new FichaInvalidaException(List.of("El tipo de especie no existe.")));

        FichaTaxonomica.Builder builder = FichaTaxonomica.builder(tipo)
                .paraHallazgo(hallazgoId)
                .version(version)
                .fechaVersion(OffsetDateTime.now(reloj))
                .reino(datos.reino())
                .filo(datos.filo())
                .clase(datos.clase())
                .orden(datos.orden())
                .familia(datos.familia())
                .genero(datos.genero())
                .especie(datos.especie());
        datos.valores().forEach((definicionId, valor) -> {
            boolean esArchivo = tipo.definicion(definicionId)
                    .map(d -> d.getTipoDato() == TipoDato.ARCHIVO)
                    .orElse(false);
            if (!esArchivo) {
                builder.atributo(definicionId, valor);
            }
        });
        archivos.forEach(builder::atributo);
        return builder.build();
    }

    public void guardar(FichaTaxonomica ficha) {
        fichas.guardar(ficha);
    }

    public Optional<FichaTaxonomica> vigente(UUID hallazgoId) {
        return fichas.buscarVigente(hallazgoId);
    }
}
