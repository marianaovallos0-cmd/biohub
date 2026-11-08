package co.edu.unbosque.biohub.publica.consultapublica.aplicacion;

import co.edu.unbosque.biohub.publica.consultapublica.dominio.BusquedaInvalidaException;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.RepositorioConsultaPublica;
import co.edu.unbosque.biohub.publica.consultapublica.dominio.ResultadoBusqueda;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * HU-09: buscar especies y hallazgos por nombre de especie.
 * El resultado se guarda en Redis 60 segundos (application-publica.yaml).
 */
@Service
public class BuscarEspeciesYHallazgos {

    public static final String CACHE = "busqueda-publica";
    public static final int MINIMO_CARACTERES = 2;
    public static final int MAXIMO_CARACTERES = 100;
    public static final int LIMITE_RESULTADOS = 50;

    private final RepositorioConsultaPublica repositorio;

    public BuscarEspeciesYHallazgos(RepositorioConsultaPublica repositorio) {
        this.repositorio = repositorio;
    }

    // La llave es el texto ya normalizado: "Ara Macao" y " ara macao " comparten caché.
    @Cacheable(cacheNames = CACHE, key = "T(co.edu.unbosque.biohub.publica.consultapublica.aplicacion.BuscarEspeciesYHallazgos).normalizar(#texto)")
    public List<ResultadoBusqueda> ejecutar(String texto) {
        String limpio = normalizar(texto);
        if (limpio.length() < MINIMO_CARACTERES) {
            throw new BusquedaInvalidaException(
                    "Escribe al menos " + MINIMO_CARACTERES + " letras del nombre de la especie.");
        }
        if (limpio.length() > MAXIMO_CARACTERES) {
            throw new BusquedaInvalidaException(
                    "La búsqueda no puede tener más de " + MAXIMO_CARACTERES + " caracteres.");
        }
        return List.copyOf(repositorio.buscarPorNombreEspecie(limpio, LIMITE_RESULTADOS));
    }

    /** Quita espacios de más y pasa a minúsculas. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
