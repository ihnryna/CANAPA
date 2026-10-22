
package org.spring.canapa.backend.voting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.movie.repository.MovieRepository;
import org.spring.canapa.backend.room.Room;
import org.spring.canapa.backend.room.RoomRepository;
import org.spring.canapa.backend.user.User;
import org.spring.canapa.backend.user.UserRepository;
import org.spring.canapa.backend.voting.entity.ExcludedMovie;
import org.spring.canapa.backend.voting.entity.Vote;
import org.spring.canapa.backend.voting.entity.VotingResult;
import org.spring.canapa.backend.voting.repository.ExcludedMovieRepository;
import org.spring.canapa.backend.voting.repository.VoteRepository;
import org.spring.canapa.backend.voting.repository.VotingResultRepository;
import org.spring.canapa.backend.voting.service.VotingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Testcontainers
class VotingServiceRepositoryIntegrationTest {

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
    private VotingService votingService;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VotingResultRepository votingResultRepository;

    @Autowired
    private ExcludedMovieRepository excludedMovieRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private MovieRepository movieRepository;

    private User user1;
    private User user2;
    private User user3;

    private Room room;
    private Room room2;

    private Movie movie1;
    private Movie movie2;
    private Movie movie3;

    private UUID roomId;
    private UUID room2Id;

    private UUID user1Id;
    private UUID user2Id;
    private UUID user3Id;

    private Long movie1Id;
    private Long movie2Id;
    private Long movie3Id;

    @BeforeEach
    void setUp() {
        voteRepository.deleteAll();
        excludedMovieRepository.deleteAll();
        votingResultRepository.deleteAll();

        user1 = saveUser("User One");
        user2 = saveUser("User Two");
        user3 = saveUser("User Three");

        user1Id = user1.getId();
        user2Id = user2.getId();
        user3Id = user3.getId();

        room = roomRepository.save(
            new Room("Voting Test Room", user1)
        );
        room2 = roomRepository.save(
            new Room("Second Voting Test Room", user2)
        );

        roomId = room.getId();
        room2Id = room2.getId();

        movie1 = saveMovie("Test Movie One", 910001L);
        movie2 = saveMovie("Test Movie Two", 910002L);
        movie3 = saveMovie("Test Movie Three", 910003L);

        movie1Id = movie1.getId();
        movie2Id = movie2.getId();
        movie3Id = movie3.getId();
    }

    private User saveUser(String name) {
        return userRepository.save(
            new User(name, "voting-" + UUID.randomUUID() + "@example.com")
        );
    }

    private Movie saveMovie(String title, Long tmdbId) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setTmdbId(tmdbId);
        return movieRepository.save(movie);
    }

    // --------------------------------------------------
    // Voting
    // --------------------------------------------------

    @Test
    void addVote_shouldSaveVote() {
        Vote vote = votingService.addVote(user1Id, roomId, movie1Id);

        assertNotNull(vote.getId());
        assertEquals(user1Id, vote.getUser().getId());
        assertEquals(roomId, vote.getRoom().getId());
        assertEquals(movie1Id, vote.getMovie().getId());

        assertEquals(1, voteRepository.findAllByRoom_Id(roomId).size());
    }

    @Test
    void addVote_shouldNotCreateDuplicateVote() {
        Vote firstVote = votingService.addVote(user1Id, roomId, movie1Id);
        Vote secondVote = votingService.addVote(user1Id, roomId, movie1Id);

        assertEquals(firstVote.getId(), secondVote.getId());
        assertEquals(1, voteRepository.findAllByRoom_Id(roomId).size());
    }

    @Test
    void differentUsers_shouldBeAbleToVoteForSameMovie() {
        votingService.addVote(user1Id, roomId, movie1Id);
        votingService.addVote(user2Id, roomId, movie1Id);

        assertEquals(2, voteRepository.findAllByRoom_Id(roomId).size());
    }

    @Test
    void removeVote_shouldDeleteOnlyUsersVote() {
        votingService.addVote(user1Id, roomId, movie1Id);
        votingService.addVote(user2Id, roomId, movie1Id);

        votingService.removeVote(user1Id, roomId, movie1Id);

        assertFalse(voteRepository.existsByUser_IdAndRoom_IdAndMovie_Id(
            user1Id, roomId, movie1Id
        ));

        assertTrue(voteRepository.existsByUser_IdAndRoom_IdAndMovie_Id(
            user2Id, roomId, movie1Id
        ));
    }

    @Test
    void getRoomVotes_shouldReturnOnlyVotesFromRequestedRoom() {
        votingService.addVote(user1Id, roomId, movie1Id);
        votingService.addVote(user2Id, roomId, movie2Id);
        votingService.addVote(user3Id, room2Id, movie3Id);

        List<Vote> votes = votingService.getRoomVotes(roomId);

        assertEquals(2, votes.size());
        assertTrue(votes.stream().allMatch(
            vote -> roomId.equals(vote.getRoom().getId())
        ));
    }

    @Test
    void getUserVotes_shouldReturnOnlyUsersVotesInRequestedRoom() {
        votingService.addVote(user1Id, roomId, movie1Id);
        votingService.addVote(user1Id, roomId, movie2Id);
        votingService.addVote(user1Id, room2Id, movie3Id);
        votingService.addVote(user2Id, roomId, movie3Id);

        List<Vote> votes = votingService.getUserVotes(user1Id, roomId);

        assertEquals(2, votes.size());
        assertTrue(votes.stream().allMatch(
            vote -> user1Id.equals(vote.getUser().getId())
                && roomId.equals(vote.getRoom().getId())
        ));
    }

    // --------------------------------------------------
    // Excluded movies
    // --------------------------------------------------

    @Test
    void excludeMovie_shouldSaveExclusion() {
        ExcludedMovie excludedMovie =
            votingService.excludeMovie(user1Id, roomId, movie1Id);

        assertNotNull(excludedMovie.getId());
        assertEquals(roomId, excludedMovie.getRoom().getId());
        assertEquals(movie1Id, excludedMovie.getMovie().getId());
        assertEquals(user1Id, excludedMovie.getExcludedBy().getId());

        assertTrue(excludedMovieRepository.existsByRoom_IdAndMovie_Id(
            roomId, movie1Id
        ));
    }

    @Test
    void excludeMovie_shouldNotCreateDuplicateExclusion() {
        ExcludedMovie first = votingService.excludeMovie(
            user1Id, roomId, movie1Id
        );

        ExcludedMovie second = votingService.excludeMovie(
            user2Id, roomId, movie1Id
        );

        assertEquals(first.getId(), second.getId());
        assertEquals(user1Id, second.getExcludedBy().getId());
        assertEquals(
            1,
            excludedMovieRepository.findAllByRoom_Id(roomId).size()
        );
    }

    @Test
    void addVote_shouldRejectExcludedMovie() {
        votingService.excludeMovie(user1Id, roomId, movie1Id);

        assertThrows(
            IllegalStateException.class,
            () -> votingService.addVote(user2Id, roomId, movie1Id)
        );
    }

    @Test
    void restoreMovie_shouldAllowUserWhoExcludedIt() {
        votingService.excludeMovie(user1Id, roomId, movie1Id);

        votingService.restoreMovie(user1Id, roomId, movie1Id);

        assertFalse(excludedMovieRepository.existsByRoom_IdAndMovie_Id(
            roomId, movie1Id
        ));
    }

    @Test
    void restoreMovie_shouldRejectDifferentUser() {
        votingService.excludeMovie(user1Id, roomId, movie1Id);

        assertThrows(
            IllegalStateException.class,
            () -> votingService.restoreMovie(user2Id, roomId, movie1Id)
        );

        assertTrue(excludedMovieRepository.existsByRoom_IdAndMovie_Id(
            roomId, movie1Id
        ));
    }

    @Test
    void restoreMovie_shouldRejectMovieThatIsNotExcluded() {
        assertThrows(
            IllegalArgumentException.class,
            () -> votingService.restoreMovie(user1Id, roomId, movie1Id)
        );
    }

    @Test
    void restoreMovie_shouldPreservePreviousVotes() {
        votingService.addVote(user1Id, roomId, movie1Id);
        votingService.excludeMovie(user1Id, roomId, movie1Id);

        votingService.restoreMovie(user1Id, roomId, movie1Id);

        assertTrue(voteRepository.existsByUser_IdAndRoom_IdAndMovie_Id(
            user1Id, roomId, movie1Id
        ));
    }

    // --------------------------------------------------
    // Voting results
    // --------------------------------------------------

    @Test
    void finishVoting_shouldSaveSingleWinner() {
        votingService.addVote(user1Id, roomId, movie1Id);
        votingService.addVote(user2Id, roomId, movie1Id);
        votingService.addVote(user3Id, roomId, movie2Id);

        VotingResult result = votingService.finishVoting(roomId);

        assertNotNull(result.getId());
        assertEquals(roomId, result.getRoom().getId());
        assertEquals(List.of(movie1Id), winnerIds(result));
        assertEquals(2, result.getVoteCount());
        assertNotNull(result.getFinishedAt());

        VotingResult saved = votingResultRepository
            .findByRoom_Id(roomId)
            .orElseThrow();

        assertEquals(List.of(movie1Id), winnerIds(saved));
        assertEquals(2, saved.getVoteCount());
    }

    @Test
    void finishVoting_shouldSaveAllWinnersInCaseOfTie() {
        votingService.addVote(user1Id, roomId, movie1Id);
        votingService.addVote(user2Id, roomId, movie1Id);

        votingService.addVote(user1Id, roomId, movie2Id);
        votingService.addVote(user2Id, roomId, movie2Id);

        votingService.addVote(user3Id, roomId, movie3Id);

        VotingResult result = votingService.finishVoting(roomId);

        assertEquals(
            List.of(movie1Id, movie2Id),
            winnerIds(result)
        );
        assertEquals(2, result.getVoteCount());
    }

    @Test
    void finishVoting_shouldSaveEmptyWinnersWhenThereAreNoVotes() {
        VotingResult result = votingService.finishVoting(roomId);

        assertNotNull(result.getId());
        assertEquals(List.of(), winnerIds(result));
        assertEquals(0, result.getVoteCount());
    }

    @Test
    void finishVoting_shouldIgnoreVotesForExcludedMovies() {
        votingService.addVote(user1Id, roomId, movie1Id);
        votingService.addVote(user2Id, roomId, movie1Id);
        votingService.addVote(user1Id, roomId, movie2Id);

        votingService.excludeMovie(user1Id, roomId, movie1Id);

        VotingResult result = votingService.finishVoting(roomId);

        assertEquals(List.of(movie2Id), winnerIds(result));
        assertEquals(1, result.getVoteCount());
    }

    @Test
    void finishVoting_shouldReturnNoWinnersWhenAllVotedMoviesAreExcluded() {
        votingService.addVote(user1Id, roomId, movie1Id);
        votingService.addVote(user2Id, roomId, movie2Id);

        votingService.excludeMovie(user1Id, roomId, movie1Id);
        votingService.excludeMovie(user2Id, roomId, movie2Id);

        VotingResult result = votingService.finishVoting(roomId);

        assertEquals(List.of(), winnerIds(result));
        assertEquals(0, result.getVoteCount());
    }

    @Test
    void finishVoting_shouldRejectSecondAttempt() {
        votingService.addVote(user1Id, roomId, movie1Id);
        votingService.finishVoting(roomId);

        assertThrows(
            IllegalStateException.class,
            () -> votingService.finishVoting(roomId)
        );
    }

    @Test
    void addVote_shouldRejectChangesAfterVotingIsFinished() {
        votingService.finishVoting(roomId);

        assertThrows(
            IllegalStateException.class,
            () -> votingService.addVote(user1Id, roomId, movie1Id)
        );
    }

    @Test
    void removeVote_shouldRejectChangesAfterVotingIsFinished() {
        votingService.addVote(user1Id, roomId, movie1Id);
        votingService.finishVoting(roomId);

        assertThrows(
            IllegalStateException.class,
            () -> votingService.removeVote(user1Id, roomId, movie1Id)
        );
    }

    @Test
    void excludeMovie_shouldRejectChangesAfterVotingIsFinished() {
        votingService.finishVoting(roomId);

        assertThrows(
            IllegalStateException.class,
            () -> votingService.excludeMovie(user1Id, roomId, movie1Id)
        );
    }

    @Test
    void restoreMovie_shouldRejectChangesAfterVotingIsFinished() {
        votingService.excludeMovie(user1Id, roomId, movie1Id);
        votingService.finishVoting(roomId);

        assertThrows(
            IllegalStateException.class,
            () -> votingService.restoreMovie(user1Id, roomId, movie1Id)
        );
    }

    // --------------------------------------------------
    // Database constraints
    // --------------------------------------------------

    @Test
    void votingResult_shouldNotAllowTwoResultsForSameRoom() {
        VotingResult first = new VotingResult(
            room, List.of(movie1), 2
        );
        votingResultRepository.saveAndFlush(first);

        VotingResult second = new VotingResult(
            room, List.of(movie2), 1
        );

        assertThrows(
            DataIntegrityViolationException.class,
            () -> votingResultRepository.saveAndFlush(second)
        );
    }

    private List<Long> winnerIds(VotingResult result) {
        return result.getWinners()
            .stream()
            .map(Movie::getId)
            .toList();
    }
}
