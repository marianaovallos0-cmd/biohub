package co.edu.unbosque.biohub.publica.integracion.aplicacion;

import co.edu.unbosque.biohub.publica.integracion.dominio.ClaveInvalidaException;
import co.edu.unbosque.biohub.publica.integracion.dominio.ConsumidorExterno;
import co.edu.unbosque.biohub.publica.integracion.dominio.RepositorioConsumidores;
import org.springframework.stereotype.Service;

/**
 * HU-16: identifica al consumidor externo por su clave (encabezado X-API-Key).
 * Se lee de la base en cada petición, así que si se crea o se quita una clave
 * aplica de inmediato, sin reiniciar.
 */
@Service
public class AutenticarConsumidor {

    private final RepositorioConsumidores consumidores;

    public AutenticarConsumidor(RepositorioConsumidores consumidores) {
        this.consumidores = consumidores;
    }

    public ConsumidorExterno ejecutar(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ClaveInvalidaException("Falta el encabezado X-API-Key.");
        }
        return consumidores.buscarPorApiKey(apiKey.trim())
                .orElseThrow(() -> new ClaveInvalidaException("La clave X-API-Key no es válida."));
    }
}
