package co.edu.unbosque.biohub.interna.usuarios.web;

import co.edu.unbosque.biohub.interna.usuarios.dominio.ConflictoException;
import co.edu.unbosque.biohub.interna.usuarios.dominio.DatosInvalidosException;
import co.edu.unbosque.biohub.interna.usuarios.dominio.NoEncontradoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Traduce los errores de la HU-15 a respuestas con status, title y detail. Solo aplica a UsuariosController. */
@RestControllerAdvice(assignableTypes = UsuariosController.class)
class ManejadorErroresUsuarios {

    @ExceptionHandler(DatosInvalidosException.class)
    ProblemDetail datosInvalidos(DatosInvalidosException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NoEncontradoException.class)
    ProblemDetail noEncontrado(NoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    // Por si dos administradores crean el mismo correo al mismo tiempo: lo frena el UNIQUE de la base
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Los datos chocan con otro registro, por ejemplo un correo que ya existe.");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail malFormado(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "La solicitud no tiene un formato válido.");
    }
}
