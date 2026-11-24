package co.edu.unbosque.biohub.publica.integracion.dominio;

import java.util.Optional;

/** Lo que la integración necesita saber de los consumidores externos. */
public interface RepositorioConsumidores {

    Optional<ConsumidorExterno> buscarPorApiKey(String apiKey);
}
