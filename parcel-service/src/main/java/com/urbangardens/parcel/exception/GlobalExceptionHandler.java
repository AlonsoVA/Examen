package com.urbangardens.parcel.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.urbangardens.parcel.dto.ApiErrorResponse;
import com.urbangardens.parcel.dto.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ErrorCodes.RESOURCE_NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidData(InvalidDataException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_DATA, ex.getMessage(), request, ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        List<FieldErrorDetail> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError -> details.add(new FieldErrorDetail(
                fieldError.getField(),
                messageOrDefault(fieldError.getDefaultMessage()))));
        ex.getBindingResult().getGlobalErrors().forEach(error -> details.add(new FieldErrorDetail(
                "request",
                messageOrDefault(error.getDefaultMessage()))));
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                "Los datos proporcionados no son válidos.", request, details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidation(HandlerMethodValidationException ex,
                                                                   HttpServletRequest request) {
        List<FieldErrorDetail> details = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String parameterName = result.getMethodParameter().getParameterName();
            String field = parameterName == null || parameterName.isBlank() ? "id" : parameterName;
            result.getResolvableErrors().forEach(error -> details.add(new FieldErrorDetail(
                    field,
                    messageOrDefault(error.getDefaultMessage()))));
        });
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                "Los datos proporcionados no son válidos.", request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                      HttpServletRequest request) {
        List<FieldErrorDetail> details = ex.getConstraintViolations().stream()
                .map(violation -> new FieldErrorDetail(fieldName(violation), violation.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                "Los datos proporcionados no son válidos.", request, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                             HttpServletRequest request) {
        InvalidFormatException invalidFormat = findCause(ex, InvalidFormatException.class);
        if (invalidFormat != null && invalidFormat.getTargetType() != null && invalidFormat.getTargetType().isEnum()) {
            return build(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_DATA,
                    "El tipo de suelo proporcionado no es válido.",
                    request,
                    List.of(new FieldErrorDetail(
                            fieldPath(invalidFormat),
                            "Use uno de los valores permitidos: CLAY (arcilloso), SILT (limoso) o SAND (arenoso).")));
        }
        if (invalidFormat != null) {
            return build(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                    "El tipo de dato proporcionado no es válido.",
                    request,
                    List.of(new FieldErrorDetail(
                            fieldPath(invalidFormat),
                            "El valor no corresponde al tipo de dato esperado.")));
        }
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.MALFORMED_REQUEST,
                "El cuerpo de la solicitud no tiene un formato JSON válido.",
                request,
                List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request) {
        String field = ex.getName() == null ? "id" : ex.getName();
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                "El tipo de dato proporcionado no es válido.",
                request,
                List.of(new FieldErrorDetail(field, "El identificador debe ser un número entero positivo.")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                HttpServletRequest request) {
        log.warn("Conflicto de integridad al procesar {}", request.getRequestURI());
        return build(HttpStatus.CONFLICT, ErrorCodes.DATA_INTEGRITY_ERROR,
                "No fue posible guardar la información por un conflicto de integridad de datos.",
                request,
                List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ErrorCodes.RESOURCE_NOT_FOUND,
                "No se encontró el recurso solicitado.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error interno al procesar {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR,
                "Ocurrió un error interno. Intente nuevamente más tarde.",
                request,
                List.of());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status,
                                                   String error,
                                                   String message,
                                                   HttpServletRequest request,
                                                   List<FieldErrorDetail> details) {
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS),
                status.value(),
                error,
                message,
                request.getRequestURI(),
                details == null ? List.of() : details);
        return ResponseEntity.status(status).body(body);
    }

    private String fieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
        int separator = path.lastIndexOf('.');
        String field = separator >= 0 ? path.substring(separator + 1) : path;
        return field.isBlank() ? "id" : field;
    }

    private String messageOrDefault(String message) {
        return message == null || message.isBlank() ? "El valor proporcionado no es válido." : message;
    }

    private String fieldPath(InvalidFormatException ex) {
        String path = ex.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining("."));
        return path.isBlank() ? "body" : path;
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
