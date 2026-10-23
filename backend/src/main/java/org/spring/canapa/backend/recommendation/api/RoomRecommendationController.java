package org.spring.canapa.backend.recommendation.api;

import org.spring.canapa.backend.recommendation.RoomRecommendationService;
import org.spring.canapa.backend.recommendation.dto.RoomRecommendedMovie;
import org.spring.canapa.backend.recommendation.strategy.RecommendationType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms/{roomId}/recommendations")
public class RoomRecommendationController {

    private final RoomRecommendationService recommendationService;

    public RoomRecommendationController(
        RoomRecommendationService recommendationService
    ) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<RoomRecommendedMovie> getRecommendations(
        @PathVariable UUID roomId,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestParam(defaultValue = "CONSENSUS")
        RecommendationType strategy,
        @RequestParam(defaultValue = "40") int limit
    ) {
        return recommendationService.recommend(
            roomId,
            userId,
            strategy,
            limit
        );
    }
}
