package org.spring.canapa.backend.invitation.exception;

public class InvitationExpiredException extends RuntimeException {

    public InvitationExpiredException(String code) {
        super("Invitation has expired: " + code);
    }
}