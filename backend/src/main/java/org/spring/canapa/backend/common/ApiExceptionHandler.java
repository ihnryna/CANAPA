package org.spring.canapa.backend.common;

import jakarta.validation.ConstraintViolationException;
import org.spring.canapa.backend.movie.provider.exception.MovieProviderException;
import org.spring.canapa.backend.movie.service.exception.MovieNotFoundException;
import org.spring.canapa.backend.preference.service.exception.UserPreferenceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MovieNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleMovieNotFound(
        MovieNotFoundException exception
    ) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(UserPreferenceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handlePreferenceNotFound(
        UserPreferenceNotFoundException exception
    ) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(
        Exception exception
    ) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
        MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));

        return problem(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MovieProviderException.class)
    public ResponseEntity<ProblemDetail> handleProviderFailure(
        MovieProviderException exception
    ) {
        return problem(
            HttpStatus.BAD_GATEWAY,
            "Movie provider request failed: " + exception.getMessage()
        );
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ProblemDetail> handleExternalHttpError(
        RestClientResponseException exception
    ) {
        return problem(
            HttpStatus.BAD_GATEWAY,
            "External movie service returned HTTP "
                + exception.getStatusCode().value()
        );
    }

    private ResponseEntity<ProblemDetail> problem(
        HttpStatus status,
        String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            status,
            detail == null ? status.getReasonPhrase() : detail
        );

        problem.setType(URI.create(
            "https://canapa.example/problems/" + status.value() // TODO: change URI
        ));

        problem.setTitle(status.getReasonPhrase());

        return ResponseEntity.status(status).body(problem);
    }
}
