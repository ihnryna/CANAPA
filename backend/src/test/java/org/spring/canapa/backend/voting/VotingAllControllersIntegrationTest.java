package org.spring.canapa.backend.voting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Testcontainers
class VotingAllControllersIntegrationTest {


    @Container
    static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("canapa_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name",
            () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    private static final UUID ROOM_1 =
        UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID ROOM_2 =
        UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final UUID ROOM_3 =
        UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
    private static final UUID ROOM_4 =
        UUID.fromString("550e8400-e29b-41d4-a716-446655440004");
    private static final UUID ROOM_5 =
        UUID.fromString("550e8400-e29b-41d4-a716-446655440005");
    private static final UUID ROOM_6 =
        UUID.fromString("550e8400-e29b-41d4-a716-446655440006");
    private static final UUID ROOM_7 =
        UUID.fromString("550e8400-e29b-41d4-a716-446655440007");
    private static final UUID ROOM_8 =
        UUID.fromString("550e8400-e29b-41d4-a716-446655440008");
    private static final UUID ROOM_9 =
        UUID.fromString("550e8400-e29b-41d4-a716-446655440009");
    private static final UUID ROOM_10 =
        UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    private static final UUID ROOM_11 =
        UUID.fromString("550e8400-e29b-41d4-a716-446655440011");
    private static final UUID ROOM_12 =
        UUID.fromString("550e8400-e29b-41d4-a716-446655440012");

    private static final UUID USER_1 =
        UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID USER_2 =
        UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    private static final UUID USER_3 =
        UUID.fromString("123e4567-e89b-12d3-a456-426614174002");

    @BeforeEach
    void setUp() {
        // Create users required by voting requests.
        jdbcTemplate.update("""
                INSERT INTO users (id, name, email, created_at)
                VALUES
                    (?, 'Test User 1', 'voting-test-1@example.com', CURRENT_TIMESTAMP),
                    (?, 'Test User 2', 'voting-test-2@example.com', CURRENT_TIMESTAMP),
                    (?, 'Test User 3', 'voting-test-3@example.com', CURRENT_TIMESTAMP)
                """,
            USER_1, USER_2, USER_3
        );

        // Create rooms used by the controller tests.
        for (UUID roomId : List.of(
            ROOM_1, ROOM_2, ROOM_3, ROOM_4,
            ROOM_5, ROOM_6, ROOM_7, ROOM_8,
            ROOM_9, ROOM_10, ROOM_11, ROOM_12
        )) {
            jdbcTemplate.update("""
                    INSERT INTO rooms (
                        id, name, creator_id, status, created_at
                    )
                    VALUES (?, ?, ?, 'VOTING', CURRENT_TIMESTAMP)
                    """,
                roomId,
                "Test Room " + roomId,
                USER_1
            );
        }

        jdbcTemplate.update("""
            INSERT INTO movies (id, title)
            VALUES
                (15, 'Test Movie 15'),
                (16, 'Test Movie 16')
            """);

        jdbcTemplate.execute("""
            SELECT setval(
                pg_get_serial_sequence('movies', 'id'),
                GREATEST((SELECT COALESCE(MAX(id), 1) FROM movies), 1)
            )
            """);
    }

    @Test
    void addVote_shouldReturnCreatedVote() throws Exception {
        mockMvc.perform(post("/api/rooms/" + ROOM_1 + "/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "userId": "%s",
                        "movieId": 15
                    }
                    """.formatted(USER_1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(USER_1.toString()))
            .andExpect(jsonPath("$.roomId").value(ROOM_1.toString()))
            .andExpect(jsonPath("$.movieId").value(15))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void getRoomVotes_shouldReturnAllVotes() throws Exception {
        addVote(ROOM_2, USER_1, 15L);
        addVote(ROOM_2, USER_2, 16L);

        mockMvc.perform(get("/api/rooms/" + ROOM_2 + "/votes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].movieId", hasItem(15)))
            .andExpect(jsonPath("$[*].movieId", hasItem(16)));
    }

    @Test
    void getUserVotes_shouldReturnOnlyVotesOfRequestedUser()
        throws Exception {
        addVote(ROOM_3, USER_1, 15L);
        addVote(ROOM_3, USER_2, 16L);

        mockMvc.perform(get("/api/rooms/" + ROOM_3 + "/votes/me")
                .param("userId", USER_1.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userId").value(USER_1.toString()))
            .andExpect(jsonPath("$[0].movieId").value(15));
    }

    @Test
    void removeVote_shouldReturnNoContent() throws Exception {
        addVote(ROOM_4, USER_1, 15L);

        mockMvc.perform(delete("/api/rooms/" + ROOM_4 + "/votes/15")
                .param("userId", USER_1.toString()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/rooms/" + ROOM_4 + "/votes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void excludeMovie_shouldReturnExcludedMovie() throws Exception {
        mockMvc.perform(post("/api/rooms/" + ROOM_5 + "/excluded-movies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "userId": "%s",
                        "movieId": 15
                    }
                    """.formatted(USER_1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roomId").value(ROOM_5.toString()))
            .andExpect(jsonPath("$.movieId").value(15))
            .andExpect(jsonPath("$.excludedByUserId")
                .value(USER_1.toString()))
            .andExpect(jsonPath("$.excludedAt").exists());
    }

    @Test
    void getExcludedMovies_shouldReturnExcludedMovies()
        throws Exception {
        excludeMovie(ROOM_6, USER_1, 15L);
        excludeMovie(ROOM_6, USER_2, 16L);

        mockMvc.perform(get("/api/rooms/" + ROOM_6 + "/excluded-movies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].movieId", hasItem(15)))
            .andExpect(jsonPath("$[*].movieId", hasItem(16)));
    }

    @Test
    void restoreMovie_shouldReturnNoContent() throws Exception {
        excludeMovie(ROOM_7, USER_1, 15L);

        mockMvc.perform(delete(
                "/api/rooms/" + ROOM_7 + "/excluded-movies/15")
                .param("userId", USER_1.toString()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/rooms/" + ROOM_7 + "/excluded-movies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void restoreMovie_shouldReturnConflictWhenAnotherUserExcludedIt()
        throws Exception {
        excludeMovie(ROOM_8, USER_1, 15L);

        mockMvc.perform(delete(
                "/api/rooms/" + ROOM_8 + "/excluded-movies/15")
                .param("userId", USER_2.toString()))
            .andExpect(status().isConflict());
    }

    @Test
    void finishVoting_shouldReturnWinner() throws Exception {
        addVote(ROOM_9, USER_1, 15L);
        addVote(ROOM_9, USER_2, 15L);
        addVote(ROOM_9, USER_3, 16L);

        mockMvc.perform(post(
                "/api/rooms/" + ROOM_9 + "/voting-result"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roomId").value(ROOM_9.toString()))
            .andExpect(jsonPath("$.winnerMovieIds.length()").value(1))
            .andExpect(jsonPath("$.winnerMovieIds[0]").value(15))
            .andExpect(jsonPath("$.winners.length()").value(1))
            .andExpect(jsonPath("$.winners[0].id").value(15))
            .andExpect(jsonPath("$.winners[0].title")
                .value("Test Movie 15"))
            .andExpect(jsonPath("$.voteCount").value(2))
            .andExpect(jsonPath("$.finishedAt").exists());

        mockMvc.perform(get("/api/rooms/" + ROOM_9))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void getVotingResult_shouldReturnSavedResult() throws Exception {
        addVote(ROOM_10, USER_1, 15L);

        mockMvc.perform(post(
                "/api/rooms/" + ROOM_10 + "/voting-result"))
            .andExpect(status().isOk());

        mockMvc.perform(get(
                "/api/rooms/" + ROOM_10 + "/voting-result"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roomId").value(ROOM_10.toString()))
            .andExpect(jsonPath("$.winnerMovieIds[0]").value(15))
            .andExpect(jsonPath("$.voteCount").value(1));
    }

    @Test
    void getVotingResult_shouldReturnNotFoundWhenVotingIsNotFinished()
        throws Exception {
        mockMvc.perform(get(
                "/api/rooms/" + ROOM_11 + "/voting-result"))
            .andExpect(status().isNotFound());
    }

    @Test
    void addVote_shouldReturnConflictAfterVotingIsFinished()
        throws Exception {
        mockMvc.perform(post(
                "/api/rooms/" + ROOM_12 + "/voting-result"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/rooms/" + ROOM_12 + "/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "userId": "%s",
                        "movieId": 15
                    }
                    """.formatted(USER_1)))
            .andExpect(status().isConflict());
    }

    private void addVote(UUID roomId, UUID userId, Long movieId)
        throws Exception {
        mockMvc.perform(post("/api/rooms/" + roomId + "/votes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "userId": "%s",
                        "movieId": %d
                    }
                    """.formatted(userId, movieId)))
            .andExpect(status().isOk());
    }

    private void excludeMovie(UUID roomId, UUID userId, Long movieId)
        throws Exception {
        mockMvc.perform(post(
                "/api/rooms/" + roomId + "/excluded-movies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "userId": "%s",
                        "movieId": %d
                    }
                    """.formatted(userId, movieId)))
            .andExpect(status().isOk());
    }
}
