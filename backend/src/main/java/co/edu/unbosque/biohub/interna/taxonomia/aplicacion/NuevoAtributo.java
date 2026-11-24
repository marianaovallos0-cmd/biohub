package co.edu.unbosque.biohub.interna.taxonomia.aplicacion;

import co.edu.unbosque.biohub.interna.taxonomia.dominio.TipoDato;

// Lo que llena el investigador para agregar un atributo (HU-03)
public record NuevoAtributo(String nombre, TipoDato tipoDato, boolean obligatorio) {
}
