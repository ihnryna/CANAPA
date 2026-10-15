package org.spring.canapa.backend.preference.service.exception;

public class UserPreferenceNotFoundException extends RuntimeException {

    public UserPreferenceNotFoundException(String message) {
        super(message);
    }
}
