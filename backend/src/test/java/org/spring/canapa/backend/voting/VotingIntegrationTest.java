
package org.spring.canapa.backend.voting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class VotingIntegrationTest {

    @Autowired
    private VotingService votingService;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VotingResultRepository votingResultRepository;

    @Autowired
    private ExcludedMovieRepository excludedMovieRepository;

    private final Long roomId = 1L;
    private final Long userId = 10L;
    private final Long anotherUserId = 20L;
    private final Long movieId = 100L;
    private final Long anotherMovieId = 200L;

    // --------------------------------------------------
    // ADD VOTE
    // --------------------------------------------------

    @Test
    void addVote_savesVoteToDatabase() {
        Vote vote = votingService.addVote(userId, roomId, movieId);

        assertNotNull(vote.getId());
        assertEquals(userId, vote.getUserId());
        assertEquals(roomId, vote.getRoomId());
        assertEquals(movieId, vote.getMovieId());
        assertNotNull(vote.getCreatedAt());

        List<Vote> roomVotes = voteRepository.findAllByRoomId(roomId);

        assertEquals(1, roomVotes.size());
        assertEquals(vote.getId(), roomVotes.get(0).getId());
    }

    @Test
    void addVote_sameUserAndMovieTwice_doesNotCreateDuplicate() {
        Vote firstVote = votingService.addVote(userId, roomId, movieId);
        Vote secondVote = votingService.addVote(userId, roomId, movieId);

        assertEquals(firstVote.getId(), secondVote.getId());
        assertEquals(1, voteRepository.findAllByRoomId(roomId).size());
    }

    @Test
    void addVote_sameUserCanVoteForDifferentMovies() {
        votingService.addVote(userId, roomId, movieId);
        votingService.addVote(userId, roomId, anotherMovieId);

        assertEquals(2, voteRepository.findAllByRoomId(roomId).size());
        assertEquals(
                2,
                votingService.getUserVotes(userId, roomId).size()
        );
    }

    @Test
    void addVote_differentUsersCanVoteForSameMovie() {
        votingService.addVote(userId, roomId, movieId);
        votingService.addVote(anotherUserId, roomId, movieId);

        assertEquals(2, voteRepository.findAllByRoomId(roomId).size());
    }

    @Test
    void addVote_sameUserCanVoteInDifferentRooms() {
        votingService.addVote(userId, roomId, movieId);
        votingService.addVote(userId, 2L, movieId);

        assertEquals(1, voteRepository.findAllByRoomId(roomId).size());
        assertEquals(1, voteRepository.findAllByRoomId(2L).size());
    }

    @Test
    void addVote_whenMovieIsExcluded_throwsException() {
        votingService.excludeMovie(userId, roomId, movieId);

        assertThrows(
                RuntimeException.class,
                () -> votingService.addVote(
                        anotherUserId, roomId, movieId
                )
        );

        assertTrue(voteRepository.findAllByRoomId(roomId).isEmpty());
    }

    @Test
    void addVote_whenVotingIsFinished_throwsException() {
        saveVotingResult(roomId, movieId, 3);

        assertThrows(
                IllegalStateException.class,
                () -> votingService.addVote(userId, roomId, movieId)
        );

        assertTrue(voteRepository.findAllByRoomId(roomId).isEmpty());
    }

    // --------------------------------------------------
    // REMOVE VOTE
    // --------------------------------------------------

    @Test
    void removeVote_deletesUsersVote() {
        votingService.addVote(userId, roomId, movieId);

        votingService.removeVote(userId, roomId, movieId);

        assertTrue(voteRepository.findAllByRoomId(roomId).isEmpty());
    }

    @Test
    void removeVote_doesNotDeleteAnotherUsersVote() {
        votingService.addVote(userId, roomId, movieId);
        votingService.addVote(anotherUserId, roomId, movieId);

        votingService.removeVote(userId, roomId, movieId);

        List<Vote> remainingVotes =
                voteRepository.findAllByRoomId(roomId);

        assertEquals(1, remainingVotes.size());
        assertEquals(anotherUserId, remainingVotes.get(0).getUserId());
    }

    @Test
    void removeVote_doesNotDeleteVotesFromAnotherRoom() {
        votingService.addVote(userId, roomId, movieId);
        votingService.addVote(userId, 2L, movieId);

        votingService.removeVote(userId, roomId, movieId);

        assertTrue(voteRepository.findAllByRoomId(roomId).isEmpty());
        assertEquals(1, voteRepository.findAllByRoomId(2L).size());
    }

    @Test
    void removeVote_whenVotingIsFinished_throwsException() {
        saveVotingResult(roomId, movieId, 1);
        votingService.addVote(userId, 2L, movieId);

        assertThrows(
                IllegalStateException.class,
                () -> votingService.removeVote(
                        userId, roomId, movieId
                )
        );
    }

    // --------------------------------------------------
    // GET VOTES
    // --------------------------------------------------

    @Test
    void getRoomVotes_returnsOnlyVotesFromRequestedRoom() {
        votingService.addVote(userId, roomId, movieId);
        votingService.addVote(anotherUserId, roomId, anotherMovieId);
        votingService.addVote(userId, 2L, movieId);

        List<Vote> votes = votingService.getRoomVotes(roomId);

        assertEquals(2, votes.size());
        assertTrue(votes.stream().allMatch(
                vote -> vote.getRoomId().equals(roomId)
        ));
    }

    @Test
    void getUserVotes_returnsOnlyVotesOfRequestedUserInRoom() {
        votingService.addVote(userId, roomId, movieId);
        votingService.addVote(userId, roomId, anotherMovieId);
        votingService.addVote(anotherUserId, roomId, movieId);
        votingService.addVote(userId, 2L, movieId);

        List<Vote> votes =
                votingService.getUserVotes(userId, roomId);

        assertEquals(2, votes.size());
        assertTrue(votes.stream().allMatch(
                vote -> vote.getUserId().equals(userId)
                        && vote.getRoomId().equals(roomId)
        ));
    }

    @Test
    void getRoomVotes_returnsEmptyListWhenRoomHasNoVotes() {
        assertTrue(votingService.getRoomVotes(roomId).isEmpty());
    }

    // --------------------------------------------------
    // EXCLUDE MOVIE
    // --------------------------------------------------

    @Test
    void excludeMovie_savesExclusionToDatabase() {
        ExcludedMovie excluded = votingService.excludeMovie(
                userId, roomId, movieId
        );

        assertNotNull(excluded.getId());
        assertEquals(roomId, excluded.getRoomId());
        assertEquals(movieId, excluded.getMovieId());
        assertEquals(userId, excluded.getExcludedByUserId());
        assertNotNull(excluded.getExcludedAt());

        assertTrue(
                excludedMovieRepository
                        .existsByRoomIdAndMovieId(roomId, movieId)
        );
    }

    @Test
    void excludeMovie_doesNotDeleteExistingVotes() {
        votingService.addVote(userId, roomId, movieId);
        votingService.addVote(anotherUserId, roomId, movieId);

        votingService.excludeMovie(userId, roomId, movieId);

        assertEquals(2, voteRepository.findAllByRoomId(roomId).size());
        assertTrue(
                excludedMovieRepository
                        .existsByRoomIdAndMovieId(roomId, movieId)
        );
    }

    @Test
    void excludeMovie_whenAlreadyExcluded_keepsOriginalOwner() {
        ExcludedMovie original = votingService.excludeMovie(
                userId, roomId, movieId
        );

        ExcludedMovie repeated = votingService.excludeMovie(
                anotherUserId, roomId, movieId
        );

        assertEquals(original.getId(), repeated.getId());
        assertEquals(userId, repeated.getExcludedByUserId());
        assertEquals(1, votingService.getExcludedMovies(roomId).size());
    }

    @Test
    void excludeMovie_canExcludeDifferentMoviesInSameRoom() {
        votingService.excludeMovie(userId, roomId, movieId);
        votingService.excludeMovie(anotherUserId, roomId, anotherMovieId);

        assertEquals(2, votingService.getExcludedMovies(roomId).size());
    }

    @Test
    void excludeMovie_sameMovieCanBeExcludedInDifferentRooms() {
        votingService.excludeMovie(userId, roomId, movieId);
        votingService.excludeMovie(userId, 2L, movieId);

        assertTrue(
                excludedMovieRepository
                        .existsByRoomIdAndMovieId(roomId, movieId)
        );
        assertTrue(
                excludedMovieRepository
                        .existsByRoomIdAndMovieId(2L, movieId)
        );
    }

    @Test
    void excludeMovie_whenVotingIsFinished_throwsException() {
        saveVotingResult(roomId, movieId, 1);

        assertThrows(
                IllegalStateException.class,
                () -> votingService.excludeMovie(
                        userId, roomId, anotherMovieId
                )
        );
    }

    // --------------------------------------------------
    // RESTORE EXCLUDED MOVIE
    // --------------------------------------------------

    @Test
    void restoreMovie_removesExclusionFromDatabase() {
        votingService.excludeMovie(userId, roomId, movieId);

        votingService.restoreMovie(userId, roomId, movieId);

        assertFalse(
                excludedMovieRepository
                        .existsByRoomIdAndMovieId(roomId, movieId)
        );
    }

    @Test
    void restoreMovie_onlyExcludingUserCanRestoreMovie() {
        votingService.excludeMovie(userId, roomId, movieId);

        assertThrows(
                RuntimeException.class,
                () -> votingService.restoreMovie(
                        anotherUserId, roomId, movieId
                )
        );

        ExcludedMovie stillExcluded = excludedMovieRepository
                .findByRoomIdAndMovieId(roomId, movieId)
                .orElseThrow();

        assertEquals(userId, stillExcluded.getExcludedByUserId());
    }

    @Test
    void restoreMovie_whenMovieIsNotExcluded_throwsException() {
        assertThrows(
                RuntimeException.class,
                () -> votingService.restoreMovie(
                        userId, roomId, movieId
                )
        );
    }

    @Test
    void restoreMovie_preservesVotesCastBeforeExclusion() {
        Vote originalVote =
                votingService.addVote(userId, roomId, movieId);

        votingService.excludeMovie(userId, roomId, movieId);
        votingService.restoreMovie(userId, roomId, movieId);

        List<Vote> votes = voteRepository.findAllByRoomId(roomId);

        assertEquals(1, votes.size());
        assertEquals(originalVote.getId(), votes.get(0).getId());
        assertEquals(movieId, votes.get(0).getMovieId());
    }

    @Test
    void restoreMovie_afterRestorationOtherUsersCanVoteAgain() {
        votingService.excludeMovie(userId, roomId, movieId);
        votingService.restoreMovie(userId, roomId, movieId);

        Vote vote = votingService.addVote(
                anotherUserId, roomId, movieId
        );

        assertNotNull(vote.getId());
        assertEquals(1, voteRepository.findAllByRoomId(roomId).size());
    }

    @Test
    void restoreMovie_whenVotingIsFinished_throwsException() {
        votingService.excludeMovie(userId, 2L, movieId);
        saveVotingResult(roomId, movieId, 1);

        assertThrows(
                IllegalStateException.class,
                () -> votingService.restoreMovie(
                        userId, roomId, movieId
                )
        );
    }

    // --------------------------------------------------
    // DATABASE CONSTRAINTS
    // --------------------------------------------------

    @Test
    void voteRepository_rejectsDuplicateUserRoomAndMovie() {
        Vote first = new Vote();
        first.setUserId(userId);
        first.setRoomId(roomId);
        first.setMovieId(movieId);

        voteRepository.saveAndFlush(first);

        Vote duplicate = new Vote();
        duplicate.setUserId(userId);
        duplicate.setRoomId(roomId);
        duplicate.setMovieId(movieId);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> voteRepository.saveAndFlush(duplicate)
        );
    }

    @Test
    void excludedMovieRepository_rejectsDuplicateRoomAndMovie() {
        ExcludedMovie first = new ExcludedMovie();
        first.setRoomId(roomId);
        first.setMovieId(movieId);
        first.setExcludedByUserId(userId);

        excludedMovieRepository.saveAndFlush(first);

        ExcludedMovie duplicate = new ExcludedMovie();
        duplicate.setRoomId(roomId);
        duplicate.setMovieId(movieId);
        duplicate.setExcludedByUserId(anotherUserId);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> excludedMovieRepository.saveAndFlush(duplicate)
        );
    }

    @Test
    void votingResultRepository_allowsOnlyOneResultPerRoom() {
        saveVotingResult(roomId, movieId, 2);

        VotingResult duplicate = new VotingResult();
        duplicate.setRoomId(roomId);
        duplicate.setWinnerMovieId(anotherMovieId);
        duplicate.setVoteCount(3);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> votingResultRepository.saveAndFlush(duplicate)
        );
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    private VotingResult saveVotingResult(
            Long targetRoomId,
            Long winnerMovieId,
            int voteCount
    ) {
        VotingResult result = new VotingResult();
        result.setRoomId(targetRoomId);
        result.setWinnerMovieId(winnerMovieId);
        result.setVoteCount(voteCount);

        return votingResultRepository.saveAndFlush(result);
    }
}
