package co.edu.unbosque.biohub.publica.consultapublica.aplicacion;

import co.edu.unbosque.biohub.publica.consultapublica.dominio.FichaNoEncontradaException;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.FichaPublica;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.RepositorioFichaPublica;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * HU-10: ver la ficha pública de una especie sin iniciar sesión.
 * La ficha se guarda en Redis 60 segundos (application-publica.yaml).
 * Si no existe se lanza una excepción, y las excepciones no se guardan en caché.
 */
@Service
public class VerFichaPublica {

    public static final String CACHE = "ficha-publica";

    private final RepositorioFichaPublica repositorio;

    public VerFichaPublica(RepositorioFichaPublica repositorio) {
        this.repositorio = repositorio;
    }

    @Cacheable(cacheNames = CACHE, key = "#hallazgoId")
    public FichaPublica ejecutar(UUID hallazgoId) {
        return repositorio.buscarPorHallazgo(hallazgoId)
                .orElseThrow(() -> new FichaNoEncontradaException(hallazgoId));
    }
}
