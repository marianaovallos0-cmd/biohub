package co.edu.unbosque.biohub.interna.hallazgos.infraestructura;

import co.edu.unbosque.biohub.interna.hallazgos.dominio.Evidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface EvidenciaSpringData extends JpaRepository<Evidencia, UUID> {
}
