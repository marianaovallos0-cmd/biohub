package co.edu.unbosque.biohub.publica.integracion.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Los datos que se entregan a los consumidores externos. La implementación solo
 * puede leer la vista pública, que no tiene coordenadas.
 */
public interface RepositorioDatosExternos {

    List<HallazgoExterno> listar(int desde, int cantidad);

    Optional<HallazgoExterno> buscarPorId(UUID hallazgoId);
}
