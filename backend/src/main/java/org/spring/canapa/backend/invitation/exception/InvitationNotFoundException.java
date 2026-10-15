package org.spring.canapa.backend.invitation.exception;

public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException(String code) {
        super("Invitation not found for code: " + code);
    }
}