package org.spring.canapa.backend.preference.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.spring.canapa.backend.preference.dto.UserPreferenceRequest;
import org.spring.canapa.backend.preference.dto.UserPreferenceResponse;
import org.spring.canapa.backend.preference.service.UserPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{roomId}/preferences")
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;

    @PutMapping("/users/{userId}")
    public UserPreferenceResponse createOrUpdatePreference(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            @Valid @RequestBody UserPreferenceRequest request
    ) {
        return preferenceService.createOrUpdatePreference(
                userId,
                roomId,
                request
        );
    }

    @GetMapping("/users/{userId}")
    public UserPreferenceResponse getPreference(
            @PathVariable Long roomId,
            @PathVariable Long userId
    ) {
        return preferenceService.getPreference(userId, roomId);
    }

    @GetMapping
    public List<UserPreferenceResponse> getRoomPreferences(
            @PathVariable Long roomId
    ) {
        return preferenceService.getRoomPreferences(roomId);
    }

    @GetMapping("/summary")
    public UserPreferenceService.GroupPreferenceSummary getGroupPreferenceSummary(
            @PathVariable Long roomId
    ) {
        return preferenceService.getGroupPreferenceSummary(roomId);
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePreference(
            @PathVariable Long roomId,
            @PathVariable Long userId
    ) {
        preferenceService.deletePreference(userId, roomId);
    }
}
