package co.edu.unbosque.biohub.interna.equipos.web;

import co.edu.unbosque.biohub.interna.equipos.aplicacion.ConsultarEquipos;
import co.edu.unbosque.biohub.interna.equipos.aplicacion.EquipoRegistrado;
import co.edu.unbosque.biohub.interna.equipos.aplicacion.NuevoEquipo;
import co.edu.unbosque.biohub.interna.equipos.aplicacion.RegistrarEquipo;
import co.edu.unbosque.biohub.interna.equipos.dominio.EquipoDuplicadoException;
import co.edu.unbosque.biohub.interna.equipos.dominio.EquipoInvalidoException;
import co.edu.unbosque.biohub.interna.equipos.dominio.Responsable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

// HU-04. Solo el administrador (permiso GESTIONAR_EQUIPOS).
// POST /interno/equipos                  registra un equipo
// GET  /interno/equipos                  los equipos registrados
// GET  /interno/equipos/responsables     los investigadores que pueden ser responsables
@RestController
@RequestMapping("/interno/equipos")
public class EquiposController {

    private final RegistrarEquipo registrarEquipo;
    private final ConsultarEquipos consultarEquipos;

    public EquiposController(RegistrarEquipo registrarEquipo, ConsultarEquipos consultarEquipos) {
        this.registrarEquipo = registrarEquipo;
        this.consultarEquipos = consultarEquipos;
    }

    @PostMapping
    @PreAuthorize("@autorizacion.validarAcceso(principal, 'GESTIONAR_EQUIPOS')")
    public ResponseEntity<EquipoRegistrado> registrar(@RequestBody NuevoEquipo nuevo) {
        EquipoRegistrado registrado = registrarEquipo.ejecutar(nuevo);
        return ResponseEntity.created(URI.create("/interno/equipos/" + registrado.id())).body(registrado);
    }

    @GetMapping
    @PreAuthorize("@autorizacion.validarAcceso(principal, 'GESTIONAR_EQUIPOS')")
    public List<EquipoRegistrado> listar() {
        return consultarEquipos.todos();
    }

    @GetMapping("/responsables")
    @PreAuthorize("@autorizacion.validarAcceso(principal, 'GESTIONAR_EQUIPOS')")
    public List<Responsable> responsables() {
        return consultarEquipos.responsables();
    }

    @ExceptionHandler(EquipoInvalidoException.class)
    ProblemDetail invalido(EquipoInvalidoException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problema.setProperty("errores", e.getErrores());
        return problema;
    }

    @ExceptionHandler(EquipoDuplicadoException.class)
    ProblemDetail duplicado(EquipoDuplicadoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    // Dos registros del mismo código al mismo tiempo: lo ataja el UNIQUE de la base
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail repetidoEnLaBase(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Ya existe un equipo con esa identificación.");
    }
}
