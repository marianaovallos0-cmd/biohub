package co.edu.unbosque.biohub.interna.equipos.infraestructura;

import co.edu.unbosque.biohub.interna.equipos.dominio.DirectorioInvestigadores;
import co.edu.unbosque.biohub.interna.equipos.dominio.Responsable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Lee la tabla investigador (con el nombre del usuario) sin depender de las clases de Seguridad
@Repository
public class DirectorioInvestigadoresJpa implements DirectorioInvestigadores {

    private static final String CONSULTA = "select u.id, u.nombre, u.correo from investigador i "
            + "join usuario u on u.id = i.usuario_id where u.activo = true";

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Responsable> buscarActivo(UUID investigadorId) {
        List<?> filas = em.createNativeQuery(CONSULTA + " and u.id = :id")
                .setParameter("id", investigadorId)
                .getResultList();
        return filas.stream().findFirst().map(DirectorioInvestigadoresJpa::aResponsable);
    }

    @Override
    public List<Responsable> listarActivos() {
        List<?> filas = em.createNativeQuery(CONSULTA + " order by u.nombre").getResultList();
        return filas.stream().map(DirectorioInvestigadoresJpa::aResponsable).toList();
    }

    private static Responsable aResponsable(Object fila) {
        Object[] columnas = (Object[]) fila;
        return new Responsable((UUID) columnas[0], (String) columnas[1], (String) columnas[2]);
    }
}
