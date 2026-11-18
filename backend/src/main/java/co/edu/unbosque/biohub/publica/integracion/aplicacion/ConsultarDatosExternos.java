package co.edu.unbosque.biohub.publica.integracion.aplicacion;

import co.edu.unbosque.biohub.publica.integracion.dominio.HallazgoExterno;
import co.edu.unbosque.biohub.publica.integracion.dominio.HallazgoNoEncontradoException;
import co.edu.unbosque.biohub.publica.integracion.dominio.PaginaInvalidaException;
import co.edu.unbosque.biohub.publica.integracion.dominio.RepositorioDatosExternos;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * HU-16: los datos habilitados para los consumidores externos.
 * Se entregan por páginas para que un consumidor no pida todo de una vez,
 * y se guardan en Redis 60 segundos porque muchos consumidores piden lo mismo.
 */
@Service
public class ConsultarDatosExternos {

    public static final String CACHE_PAGINAS = "externo-hallazgos";
    public static final String CACHE_HALLAZGO = "externo-hallazgo";
    public static final int TAMANO_POR_DEFECTO = 100;
    public static final int TAMANO_MAXIMO = 500;

    private final RepositorioDatosExternos datos;

    public ConsultarDatosExternos(RepositorioDatosExternos datos) {
        this.datos = datos;
    }

    @Cacheable(cacheNames = CACHE_PAGINAS, key = "#pagina + '-' + #tamano")
    public List<HallazgoExterno> listar(int pagina, int tamano) {
        if (pagina < 0) {
            throw new PaginaInvalidaException("La página no puede ser negativa.");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new PaginaInvalidaException("El tamaño debe estar entre 1 y " + TAMANO_MAXIMO + ".");
        }
        int desde;
        try {
            desde = Math.multiplyExact(pagina, tamano);
        } catch (ArithmeticException e) {
            throw new PaginaInvalidaException("La página es demasiado grande.");
        }
        return List.copyOf(datos.listar(desde, tamano));
    }

    @Cacheable(cacheNames = CACHE_HALLAZGO, key = "#hallazgoId")
    public HallazgoExterno buscar(UUID hallazgoId) {
        return datos.buscarPorId(hallazgoId)
                .orElseThrow(() -> new HallazgoNoEncontradoException(hallazgoId));
    }
}
