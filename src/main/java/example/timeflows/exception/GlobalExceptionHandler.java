package example.timeflows.exception;

import example.timeflows.controller.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponse> handleUserException(
            UserException exception, HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "Помилка користувача",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(DepartmentException.class)
    public ResponseEntity<ErrorResponse> handleDepartmentException(
            DepartmentException exception, HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "Помилка департаменту",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(DivisionException.class)
    public ResponseEntity<ErrorResponse> handleDivisionException(
            DivisionException exception, HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "Помилка відділу",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(OvertimeException.class)
    public ResponseEntity<ErrorResponse> handleOvertimeException(
            OvertimeException exception, HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "Помилка overtime",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(
                HttpStatus.BAD_REQUEST,
                "Помилка валідації",
                "Перевірте дані запиту",
                request,
                errors);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request,
            Map<String, String> validationErrors) {
        return ResponseEntity.status(status)
                .body(
                        new ErrorResponse(
                                LocalDateTime.now(),
                                status.value(),
                                error,
                                message,
                                request.getRequestURI(),
                                validationErrors));
    }
}
