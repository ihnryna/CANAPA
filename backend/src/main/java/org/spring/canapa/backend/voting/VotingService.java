
package org.spring.canapa.backend.voting;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VotingService {

    private final VoteRepository voteRepository;
    private final VotingResultRepository votingResultRepository;
    private final ExcludedMovieRepository excludedMovieRepository;

    public VotingService(
            VoteRepository voteRepository,
            VotingResultRepository votingResultRepository,
            ExcludedMovieRepository excludedMovieRepository
    ) {
        this.voteRepository = voteRepository;
        this.votingResultRepository = votingResultRepository;
        this.excludedMovieRepository = excludedMovieRepository;
    }

    @Transactional
    public Vote addVote(Long userId, Long roomId, Long movieId) {
        ensureVotingIsOpen(roomId);

        if (excludedMovieRepository.existsByRoomIdAndMovieId(
                roomId, movieId)) {
            throw new IllegalStateException(
                    "Cannot vote for an excluded movie"
            );
        }

        return voteRepository
                .findByUserIdAndRoomIdAndMovieId(userId, roomId, movieId)
                .orElseGet(() -> {
                    Vote vote = new Vote();
                    vote.setUserId(userId);
                    vote.setRoomId(roomId);
                    vote.setMovieId(movieId);
                    return voteRepository.save(vote);
                });
    }

    @Transactional
    public void removeVote(Long userId, Long roomId, Long movieId) {
        ensureVotingIsOpen(roomId);

        voteRepository.deleteByUserIdAndRoomIdAndMovieId(
                userId, roomId, movieId
        );
    }

    @Transactional(readOnly = true)
    public List<Vote> getRoomVotes(Long roomId) {
        return voteRepository.findAllByRoomId(roomId);
    }

    @Transactional(readOnly = true)
    public List<Vote> getUserVotes(Long userId, Long roomId) {
        return voteRepository.findAllByUserIdAndRoomId(userId, roomId);
    }

    @Transactional(readOnly = true)
    public List<ExcludedMovie> getExcludedMovies(Long roomId) {
        return excludedMovieRepository.findAllByRoomId(roomId);
    }

    @Transactional
    public ExcludedMovie excludeMovie(
            Long userId,
            Long roomId,
            Long movieId
    ) {
        ensureVotingIsOpen(roomId);

        return excludedMovieRepository
                .findByRoomIdAndMovieId(roomId, movieId)
                .orElseGet(() -> {
                    ExcludedMovie excludedMovie = new ExcludedMovie();
                    excludedMovie.setRoomId(roomId);
                    excludedMovie.setMovieId(movieId);
                    excludedMovie.setExcludedByUserId(userId);

                    // Попередні голоси залишаються в БД.
                    return excludedMovieRepository.save(excludedMovie);
                });
    }

    @Transactional
    public void restoreMovie(
            Long userId,
            Long roomId,
            Long movieId
    ) {
        ensureVotingIsOpen(roomId);

        ExcludedMovie excludedMovie = excludedMovieRepository
                .findByRoomIdAndMovieId(roomId, movieId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Movie is not excluded"
                ));

        if (!excludedMovie.getExcludedByUserId().equals(userId)) {
            throw new IllegalStateException(
                    "Only the user who excluded the movie can restore it"
            );
        }

        excludedMovieRepository.delete(excludedMovie);
    }

    private void ensureVotingIsOpen(Long roomId) {
        if (votingResultRepository.existsByRoomId(roomId)) {
            throw new IllegalStateException(
                    "Voting has already finished for this room"
            );
        }
    }
}
