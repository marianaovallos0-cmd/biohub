package co.edu.unbosque.biohub.interna.historial.infraestructura;

import co.edu.unbosque.biohub.interna.historial.dominio.RegistroCambio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface CambioSpringData extends JpaRepository<RegistroCambio, UUID> {

    @Query("""
            SELECT r FROM RegistroCambio r
            WHERE r.versionNuevaId IN (
                SELECT f.id FROM FichaTaxonomica f WHERE f.hallazgoId = :hallazgoId)
            ORDER BY r.fechaHora ASC
            """)
    List<RegistroCambio> listarPorHallazgo(@Param("hallazgoId") UUID hallazgoId);
}
