package co.edu.unbosque.biohub.analitica.generador.dominio;

// Lo que se cuenta en la base para un departamento en un mes
public record ConteoDepartamento(String departamento, int total, int protegidos) {

    // Para un departamento que no tuvo hallazgos ese mes
    public static ConteoDepartamento vacio(String departamento) {
        return new ConteoDepartamento(departamento, 0, 0);
    }
}