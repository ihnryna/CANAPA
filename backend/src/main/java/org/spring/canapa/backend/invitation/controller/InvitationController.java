package org.spring.canapa.backend.invitation.controller;

import jakarta.validation.Valid;
import org.spring.canapa.backend.invitation.dto.CreateInvitationRequest;
import org.spring.canapa.backend.invitation.dto.InvitationResponse;
import org.spring.canapa.backend.invitation.entity.Invitation;
import org.spring.canapa.backend.invitation.service.InvitationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/rooms/{roomId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse createInvitation(
        @PathVariable UUID roomId,
        @RequestHeader("X-User-Id") UUID userId,
        @Valid @RequestBody(required = false)
        CreateInvitationRequest request
    ) {
        Long expiresInHours = request == null
            ? null
            : request.expiresInHours();

        Invitation invitation = invitationService.createInvitation(
            roomId,
            userId,
            expiresInHours
        );

        return InvitationResponse.from(
            invitation,
            invitationService.createInvitationLink(invitation.getCode())
        );
    }

    /*
     * TEMPORARY: validates the invitation only.
     * Actual room joining will be implemented after RoomService is integrated.
     */
    @PostMapping("/invitations/{code}/join")
    public InvitationResponse validateInvitationForJoining(
        @PathVariable String code,
        @RequestHeader("X-User-Id") UUID userId
    ) {
        Invitation invitation = invitationService.getValidInvitation(code);

        return InvitationResponse.from(
            invitation,
            invitationService.createInvitationLink(invitation.getCode())
        );
    }
}
