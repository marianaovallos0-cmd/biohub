package co.edu.unbosque.biohub.interna.historial.dominio;

// Dos auditores reclasificaron el mismo hallazgo al mismo tiempo: la base rechazó el segundo
// por la restricción única (hallazgo_id, numero_version) y no quedaron dos versiones con el mismo número.
public class VersionDuplicadaException extends RuntimeException {

    public VersionDuplicadaException() {
        super("Otra persona reclasificó este hallazgo al mismo tiempo. Revisa la versión vigente e intenta de nuevo.");
    }
}
