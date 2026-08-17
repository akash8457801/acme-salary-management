package co.acme.salary.web;

import co.acme.salary.domain.CompensationTimeline.InvalidCompensationChange;
import co.acme.salary.domain.FxRateTable.UnsupportedCurrencyException;
import co.acme.salary.domain.Money.CurrencyMismatchException;
import co.acme.salary.service.EmployeeService.DuplicateEmailException;
import co.acme.salary.service.ResourceNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns domain failures into HTTP responses the UI can act on.
 *
 * <p>The distinction that matters here is 400 versus 422. A malformed request — a missing field, a
 * negative amount — is a 400: the client sent something it should not have. A pay change that
 * would break the timeline is a 422: the request was well-formed, and the business rejected it.
 * Only the second kind is worth showing to the HR manager verbatim, and the domain messages are
 * written to be read by one.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** The error shape every failure uses, so the client has exactly one thing to handle. */
    public record ApiError(String message, Map<String, String> fieldErrors) {

        static ApiError of(String message) {
            return new ApiError(message, null);
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiError> conflict(DuplicateEmailException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler({InvalidCompensationChange.class, UnsupportedCurrencyException.class,
            CurrencyMismatchException.class})
    public ResponseEntity<ApiError> businessRuleRejected(RuntimeException exception) {
        return ResponseEntity.unprocessableEntity().body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validationFailed(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest()
                .body(new ApiError("Some fields need attention", fieldErrors));
    }
}
