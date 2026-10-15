package org.spring.canapa.backend.voting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class VotingServiceRepositoryIntegrationTest {

    @Autowired
    private VotingService votingService;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VotingResultRepository votingResultRepository;

    @Autowired
    private ExcludedMovieRepository excludedMovieRepository;

    private static final UUID ROOM_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private static final UUID ROOM_2 =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    private static final UUID USER_1 =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    private static final UUID USER_2 =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

    private static final UUID USER_3 =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174002");

    private static final Long MOVIE_1 = 11L;
    private static final Long MOVIE_2 = 22L;
    private static final Long MOVIE_3 = 33L;

    @BeforeEach
    void setUp() {
        // Тести працюють у транзакціях, тому дані не повинні
        // залишатися після завершення тесту.
        voteRepository.deleteAll();
        excludedMovieRepository.deleteAll();
        votingResultRepository.deleteAll();
    }

    // --------------------------------------------------
    // Voting
    // --------------------------------------------------

    @Test
    void addVote_shouldSaveVote() {
        Vote vote = votingService.addVote(USER_1, ROOM_ID, MOVIE_1);

        assertNotNull(vote.getId());
        assertEquals(USER_1, vote.getUserId());
        assertEquals(ROOM_ID, vote.getRoomId());
        assertEquals(MOVIE_1, vote.getMovieId());

        assertEquals(1, voteRepository.findAllByRoomId(ROOM_ID).size());
    }

    @Test
    void addVote_shouldNotCreateDuplicateVote() {
        Vote firstVote = votingService.addVote(USER_1, ROOM_ID, MOVIE_1);
        Vote secondVote = votingService.addVote(USER_1, ROOM_ID, MOVIE_1);

        assertEquals(firstVote.getId(), secondVote.getId());
        assertEquals(1, voteRepository.findAllByRoomId(ROOM_ID).size());
    }

    @Test
    void differentUsers_shouldBeAbleToVoteForSameMovie() {
        votingService.addVote(USER_1, ROOM_ID, MOVIE_1);
        votingService.addVote(USER_2, ROOM_ID, MOVIE_1);

        assertEquals(2, voteRepository.findAllByRoomId(ROOM_ID).size());
    }

    @Test
    void removeVote_shouldDeleteOnlyUsersVote() {
        votingService.addVote(USER_1, ROOM_ID, MOVIE_1);
        votingService.addVote(USER_2, ROOM_ID, MOVIE_1);

        votingService.removeVote(USER_1, ROOM_ID, MOVIE_1);

        assertFalse(voteRepository.existsByUserIdAndRoomIdAndMovieId(
                USER_1, ROOM_ID, MOVIE_1
        ));

        assertTrue(voteRepository.existsByUserIdAndRoomIdAndMovieId(
                USER_2, ROOM_ID, MOVIE_1
        ));
    }

    @Test
    void getRoomVotes_shouldReturnOnlyVotesFromRequestedRoom() {
        votingService.addVote(USER_1, ROOM_ID, MOVIE_1);
        votingService.addVote(USER_2, ROOM_ID, MOVIE_2);
        votingService.addVote(USER_3, ROOM_2, MOVIE_3);

        List<Vote> votes = votingService.getRoomVotes(ROOM_ID);

        assertEquals(2, votes.size());
        assertTrue(votes.stream().allMatch(
                vote -> ROOM_ID.equals(vote.getRoomId())
        ));
    }

    @Test
    void getUserVotes_shouldReturnOnlyUsersVotesInRequestedRoom() {
        votingService.addVote(USER_1, ROOM_ID, MOVIE_1);
        votingService.addVote(USER_1, ROOM_ID, MOVIE_2);
        votingService.addVote(USER_1, ROOM_2, MOVIE_3);
        votingService.addVote(USER_2, ROOM_ID, MOVIE_3);

        List<Vote> votes = votingService.getUserVotes(USER_1, ROOM_ID);

        assertEquals(2, votes.size());
        assertTrue(votes.stream().allMatch(
                vote -> USER_1.equals(vote.getUserId())
                        && ROOM_ID.equals(vote.getRoomId())
        ));
    }

    // --------------------------------------------------
    // Excluded movies
    // --------------------------------------------------

    @Test
    void excludeMovie_shouldSaveExclusion() {
        ExcludedMovie excludedMovie =
                votingService.excludeMovie(USER_1, ROOM_ID, MOVIE_1);

        assertNotNull(excludedMovie.getId());
        assertEquals(ROOM_ID, excludedMovie.getRoomId());
        assertEquals(MOVIE_1, excludedMovie.getMovieId());
        assertEquals(USER_1, excludedMovie.getExcludedByUserId());

        assertTrue(excludedMovieRepository.existsByRoomIdAndMovieId(
                ROOM_ID, MOVIE_1
        ));
    }

    @Test
    void excludeMovie_shouldNotCreateDuplicateExclusion() {
        ExcludedMovie first = votingService.excludeMovie(
                USER_1, ROOM_ID, MOVIE_1
        );

        ExcludedMovie second = votingService.excludeMovie(
                USER_2, ROOM_ID, MOVIE_1
        );

        assertEquals(first.getId(), second.getId());
        assertEquals(USER_1, second.getExcludedByUserId());
        assertEquals(1, excludedMovieRepository.findAllByRoomId(ROOM_ID).size());
    }

    @Test
    void addVote_shouldRejectExcludedMovie() {
        votingService.excludeMovie(USER_1, ROOM_ID, MOVIE_1);

        assertThrows(IllegalStateException.class,
                () -> votingService.addVote(USER_2, ROOM_ID, MOVIE_1));
    }

    @Test
    void restoreMovie_shouldAllowUserWhoExcludedIt() {
        votingService.excludeMovie(USER_1, ROOM_ID, MOVIE_1);

        votingService.restoreMovie(USER_1, ROOM_ID, MOVIE_1);

        assertFalse(excludedMovieRepository.existsByRoomIdAndMovieId(
                ROOM_ID, MOVIE_1
        ));
    }

    @Test
    void restoreMovie_shouldRejectDifferentUser() {
        votingService.excludeMovie(USER_1, ROOM_ID, MOVIE_1);

        assertThrows(IllegalStateException.class,
                () -> votingService.restoreMovie(USER_2, ROOM_ID, MOVIE_1));

        assertTrue(excludedMovieRepository.existsByRoomIdAndMovieId(
                ROOM_ID, MOVIE_1
        ));
    }

    @Test
    void restoreMovie_shouldRejectMovieThatIsNotExcluded() {
        assertThrows(IllegalArgumentException.class,
                () -> votingService.restoreMovie(USER_1, ROOM_ID, MOVIE_1));
    }

    @Test
    void restoreMovie_shouldPreservePreviousVotes() {
        votingService.addVote(USER_1, ROOM_ID, MOVIE_1);
        votingService.excludeMovie(USER_1, ROOM_ID, MOVIE_1);

        votingService.restoreMovie(USER_1, ROOM_ID, MOVIE_1);

        assertTrue(voteRepository.existsByUserIdAndRoomIdAndMovieId(
                USER_1, ROOM_ID, MOVIE_1
        ));
    }

    // --------------------------------------------------
    // Voting results
    // --------------------------------------------------

    @Test
    void finishVoting_shouldSaveSingleWinner() {
        votingService.addVote(USER_1, ROOM_ID, MOVIE_1);
        votingService.addVote(USER_2, ROOM_ID, MOVIE_1);
        votingService.addVote(USER_3, ROOM_ID, MOVIE_2);

        VotingResult result = votingService.finishVoting(ROOM_ID);

        assertNotNull(result.getId());
        assertEquals(ROOM_ID, result.getRoomId());
        assertEquals(List.of(MOVIE_1), result.getWinnerMovieIds());
        assertEquals(2, result.getVoteCount());
        assertNotNull(result.getFinishedAt());

        VotingResult saved = votingResultRepository
                .findByRoomId(ROOM_ID)
                .orElseThrow();

        assertEquals(List.of(MOVIE_1), saved.getWinnerMovieIds());
        assertEquals(2, saved.getVoteCount());
    }

    @Test
    void finishVoting_shouldSaveAllWinnersInCaseOfTie() {
        votingService.addVote(USER_1, ROOM_ID, MOVIE_1);
        votingService.addVote(USER_2, ROOM_ID, MOVIE_1);

        votingService.addVote(USER_1, ROOM_ID, MOVIE_2);
        votingService.addVote(USER_2, ROOM_ID, MOVIE_2);

        votingService.addVote(USER_3, ROOM_ID, MOVIE_3);

        VotingResult result = votingService.finishVoting(ROOM_ID);

        assertEquals(List.of(MOVIE_1, MOVIE_2), result.getWinnerMovieIds());
        assertEquals(2, result.getVoteCount());
    }

    @Test
    void finishVoting_shouldSaveEmptyWinnersWhenThereAreNoVotes() {
        VotingResult result = votingService.finishVoting(ROOM_ID);

        assertNotNull(result.getId());
        assertEquals(List.of(), result.getWinnerMovieIds());
        assertEquals(0, result.getVoteCount());
    }

    @Test
    void finishVoting_shouldIgnoreVotesForExcludedMovies() {
        votingService.addVote(USER_1, ROOM_ID, MOVIE_1);
        votingService.addVote(USER_2, ROOM_ID, MOVIE_1);
        votingService.addVote(USER_1, ROOM_ID, MOVIE_2);

        votingService.excludeMovie(USER_1, ROOM_ID, MOVIE_1);

        VotingResult result = votingService.finishVoting(ROOM_ID);

        assertEquals(List.of(MOVIE_2), result.getWinnerMovieIds());
        assertEquals(1, result.getVoteCount());
    }

    @Test
    void finishVoting_shouldReturnNoWinnersWhenAllVotedMoviesAreExcluded() {
        votingService.addVote(USER_1, ROOM_ID, MOVIE_1);
        votingService.addVote(USER_2, ROOM_ID, MOVIE_2);

        votingService.excludeMovie(USER_1, ROOM_ID, MOVIE_1);
        votingService.excludeMovie(USER_2, ROOM_ID, MOVIE_2);

        VotingResult result = votingService.finishVoting(ROOM_ID);

        assertEquals(List.of(), result.getWinnerMovieIds());
        assertEquals(0, result.getVoteCount());
    }

    @Test
    void finishVoting_shouldRejectSecondAttempt() {
        votingService.addVote(USER_1, ROOM_ID, MOVIE_1);
        votingService.finishVoting(ROOM_ID);

        assertThrows(IllegalStateException.class,
                () -> votingService.finishVoting(ROOM_ID));
    }

    @Test
    void addVote_shouldRejectChangesAfterVotingIsFinished() {
        votingService.finishVoting(ROOM_ID);

        assertThrows(IllegalStateException.class,
                () -> votingService.addVote(USER_1, ROOM_ID, MOVIE_1));
    }

    @Test
    void removeVote_shouldRejectChangesAfterVotingIsFinished() {
        votingService.addVote(USER_1, ROOM_ID, MOVIE_1);
        votingService.finishVoting(ROOM_ID);

        assertThrows(IllegalStateException.class,
                () -> votingService.removeVote(USER_1, ROOM_ID, MOVIE_1));
    }

    @Test
    void excludeMovie_shouldRejectChangesAfterVotingIsFinished() {
        votingService.finishVoting(ROOM_ID);

        assertThrows(IllegalStateException.class,
                () -> votingService.excludeMovie(USER_1, ROOM_ID, MOVIE_1));
    }

    @Test
    void restoreMovie_shouldRejectChangesAfterVotingIsFinished() {
        votingService.excludeMovie(USER_1, ROOM_ID, MOVIE_1);
        votingService.finishVoting(ROOM_ID);

        assertThrows(IllegalStateException.class,
                () -> votingService.restoreMovie(USER_1, ROOM_ID, MOVIE_1));
    }

    // --------------------------------------------------
    // Database constraints
    // --------------------------------------------------

    @Test
    void votingResult_shouldNotAllowTwoResultsForSameRoom() {
        VotingResult first = saveVotingResult(ROOM_ID, List.of(MOVIE_1), 2);
        votingResultRepository.saveAndFlush(first);

        VotingResult second = saveVotingResult(ROOM_ID, List.of(MOVIE_2), 1);

        assertThrows(DataIntegrityViolationException.class,
                () -> votingResultRepository.saveAndFlush(second));
    }

    private VotingResult saveVotingResult(
            UUID roomId,
            List<Long> winnerMovieIds,
            int voteCount
    ) {
        VotingResult result = new VotingResult();
        result.setRoomId(roomId);
        result.setWinnerMovieIds(winnerMovieIds);
        result.setVoteCount(voteCount);
        return result;
    }
}