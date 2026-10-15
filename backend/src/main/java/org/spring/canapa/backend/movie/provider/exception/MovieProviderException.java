package org.spring.canapa.backend.movie.provider.exception;

public class MovieProviderException extends RuntimeException {

    public MovieProviderException(String message) {
        super(message);
    }

    public MovieProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
