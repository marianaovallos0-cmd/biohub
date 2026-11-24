package co.edu.unbosque.biohub.interna.seguridad.infraestructura;

import co.edu.unbosque.biohub.interna.seguridad.dominio.IntentoAccesoNoAutorizado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface IntentoSpringData extends JpaRepository<IntentoAccesoNoAutorizado, UUID> {

        List<IntentoAccesoNoAutorizado> findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDesc(
            OffsetDateTime desde, OffsetDateTime hasta);

        }