package cl.duoc.skyrimemporium.error;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorApi> noEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<ErrorApi> sinStock(StockInsuficienteException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorApi> duplicado(RecursoDuplicadoException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorApi> credencialesInvalidas(CredencialesInvalidasException ex) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorApi> accesoDenegado(AccesoDenegadoException ex) {
        return responder(HttpStatus.FORBIDDEN, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorApi> argumentoIncorrecto(IllegalArgumentException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<ErrorApi> cantidadExcesiva(ArithmeticException ex) {
        return responder(HttpStatus.BAD_REQUEST, "La cantidad solicitada excede el límite permitido", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorApi> conflictoDeDatos(DataIntegrityViolationException ex) {
        return responder(HttpStatus.CONFLICT, "El recurso entra en conflicto con datos existentes", Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> datosInvalidos(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return responder(HttpStatus.BAD_REQUEST, "Revisa los datos marcados", campos);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorApi> tipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El valor «" + ex.getValue() + "» no es válido", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApi> cuerpoIncorrecto(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no tiene un formato válido", Map.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorApi> conflictoDeActualizacion(RuntimeException ex) {
        return responder(HttpStatus.CONFLICT, "El producto cambió mientras se procesaba la solicitud; inténtalo otra vez",
                Map.of());
    }

    private ResponseEntity<ErrorApi> responder(HttpStatus estado, String mensaje, Map<String, String> campos) {
        return ResponseEntity.status(estado).body(new ErrorApi(estado.value(), mensaje, campos));
    }
}
