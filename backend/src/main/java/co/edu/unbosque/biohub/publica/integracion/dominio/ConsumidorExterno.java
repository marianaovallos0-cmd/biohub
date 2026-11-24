package co.edu.unbosque.biohub.publica.integracion.dominio;

import java.util.UUID;

/**
 * Una universidad u ONG que consume los datos de BioHub (HU-16).
 * La apiKey no se guarda aquí a propósito: solo se usa para buscarlo y no debe
 * terminar en logs ni en respuestas.
 * limitePorMinuto lo aplica Nginx (HU-17); aquí solo se lee.
 */
public record ConsumidorExterno(UUID id, String organizacion, String tipo, int limitePorMinuto) {
}
