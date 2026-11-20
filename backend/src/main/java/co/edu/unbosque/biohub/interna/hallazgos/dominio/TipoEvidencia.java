package co.edu.unbosque.biohub.interna.hallazgos.dominio;

import java.util.Set;

// Cada tipo de evidencia acepta sus formatos
public enum TipoEvidencia {

    FOTOGRAFIA(Set.of("jpg", "jpeg", "png")),
    MUESTRA(Set.of("jpg", "jpeg", "png", "pdf")),
    NOTA_CAMPO(Set.of("jpg", "jpeg", "png", "pdf")),
    AUDIO(Set.of("mp3", "wav"));

    private final Set<String> extensiones;

    TipoEvidencia(Set<String> extensiones) {
        this.extensiones = extensiones;
    }

    public boolean aceptaExtension(String extension) {
        return extensiones.contains(extension);
    }

    public String formatos() {
        return String.join(", ", extensiones.stream().sorted().toList());
    }
}
