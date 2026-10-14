package org.spring.canapa.backend.voting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VotingAllControllersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void addVote_shouldReturnCreatedVote() throws Exception {
        mockMvc.perform(post("/api/rooms/9001/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": 101,
                                    "movieId": 15
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(101))
                .andExpect(jsonPath("$.roomId").value(9001))
                .andExpect(jsonPath("$.movieId").value(15))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void getRoomVotes_shouldReturnAllVotes() throws Exception {
        addVote(9002L, 101L, 15L);
        addVote(9002L, 102L, 16L);

        mockMvc.perform(get("/api/rooms/9002/votes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].movieId", hasItem(15)))
                .andExpect(jsonPath("$[*].movieId", hasItem(16)));
    }

    @Test
    void getUserVotes_shouldReturnOnlyVotesOfRequestedUser()
            throws Exception {
        addVote(9003L, 101L, 15L);
        addVote(9003L, 102L, 16L);

        mockMvc.perform(get("/api/rooms/9003/votes/me")
                        .param("userId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(101))
                .andExpect(jsonPath("$[0].movieId").value(15));
    }

    @Test
    void removeVote_shouldReturnNoContent() throws Exception {
        addVote(9004L, 101L, 15L);

        mockMvc.perform(delete("/api/rooms/9004/votes/15")
                        .param("userId", "101"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/rooms/9004/votes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void excludeMovie_shouldReturnExcludedMovie() throws Exception {
        mockMvc.perform(post("/api/rooms/9005/excluded-movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": 101,
                                    "movieId": 15
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(9005))
                .andExpect(jsonPath("$.movieId").value(15))
                .andExpect(jsonPath("$.excludedByUserId").value(101))
                .andExpect(jsonPath("$.excludedAt").exists());
    }

    @Test
    void getExcludedMovies_shouldReturnExcludedMovies()
            throws Exception {
        excludeMovie(9006L, 101L, 15L);
        excludeMovie(9006L, 102L, 16L);

        mockMvc.perform(get("/api/rooms/9006/excluded-movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].movieId", hasItem(15)))
                .andExpect(jsonPath("$[*].movieId", hasItem(16)));
    }

    @Test
    void restoreMovie_shouldReturnNoContent() throws Exception {
        excludeMovie(9007L, 101L, 15L);

        mockMvc.perform(delete(
                        "/api/rooms/9007/excluded-movies/15")
                        .param("userId", "101"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/rooms/9007/excluded-movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void restoreMovie_shouldReturnConflictWhenAnotherUserExcludedIt()
            throws Exception {
        excludeMovie(9008L, 101L, 15L);

        mockMvc.perform(delete(
                        "/api/rooms/9008/excluded-movies/15")
                        .param("userId", "102"))
                .andExpect(status().isConflict());
    }

    @Test
    void finishVoting_shouldReturnWinner() throws Exception {
        addVote(9009L, 101L, 15L);
        addVote(9009L, 102L, 15L);
        addVote(9009L, 103L, 16L);

        mockMvc.perform(post("/api/rooms/9009/voting-result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(9009))
                .andExpect(jsonPath("$.winnerMovieIds.length()").value(1))
                .andExpect(jsonPath("$.winnerMovieIds[0]").value(15))
                .andExpect(jsonPath("$.voteCount").value(2))
                .andExpect(jsonPath("$.finishedAt").exists());
    }

    @Test
    void getVotingResult_shouldReturnSavedResult() throws Exception {
        addVote(9010L, 101L, 15L);
        mockMvc.perform(post("/api/rooms/9010/voting-result"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/rooms/9010/voting-result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(9010))
                .andExpect(jsonPath("$.winnerMovieIds[0]").value(15))
                .andExpect(jsonPath("$.voteCount").value(1));
    }

    @Test
    void getVotingResult_shouldReturnNotFoundWhenVotingIsNotFinished()
            throws Exception {
        mockMvc.perform(get("/api/rooms/9011/voting-result"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addVote_shouldReturnConflictAfterVotingIsFinished()
            throws Exception {
        mockMvc.perform(post("/api/rooms/9012/voting-result"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/rooms/9012/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": 101,
                                    "movieId": 15
                                }
                                """))
                .andExpect(status().isConflict());
    }

    private void addVote(Long roomId, Long userId, Long movieId)
            throws Exception {
        mockMvc.perform(post("/api/rooms/" + roomId + "/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": %d,
                                    "movieId": %d
                                }
                                """.formatted(userId, movieId)))
                .andExpect(status().isOk());
    }

    private void excludeMovie(Long roomId, Long userId, Long movieId)
            throws Exception {
        mockMvc.perform(post(
                        "/api/rooms/" + roomId + "/excluded-movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userId": %d,
                                    "movieId": %d
                                }
                                """.formatted(userId, movieId)))
                .andExpect(status().isOk());
    }
}