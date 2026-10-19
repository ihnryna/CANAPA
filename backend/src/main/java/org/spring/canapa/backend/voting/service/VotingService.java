
package org.spring.canapa.backend.voting.service;

import org.spring.canapa.backend.voting.entity.ExcludedMovie;
import org.spring.canapa.backend.voting.repository.ExcludedMovieRepository;
import org.spring.canapa.backend.voting.repository.VoteRepository;
import org.spring.canapa.backend.voting.repository.VotingResultRepository;
import org.spring.canapa.backend.voting.entity.Vote;
import org.spring.canapa.backend.voting.entity.VotingResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.spring.canapa.backend.notification.event.VotingResultsReadyEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VotingService {

    private final VoteRepository voteRepository;
    private final VotingResultRepository votingResultRepository;
    private final ExcludedMovieRepository excludedMovieRepository;
    private final ApplicationEventPublisher eventPublisher;

    public VotingService(
            VoteRepository voteRepository,
            VotingResultRepository votingResultRepository,
            ExcludedMovieRepository excludedMovieRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.voteRepository = voteRepository;
        this.votingResultRepository = votingResultRepository;
        this.excludedMovieRepository = excludedMovieRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Vote addVote(UUID userId, UUID roomId, Long movieId) {
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
    public void removeVote(UUID userId, UUID roomId, Long movieId) {
        ensureVotingIsOpen(roomId);

        voteRepository.deleteByUserIdAndRoomIdAndMovieId(
                userId, roomId, movieId
        );
    }

    @Transactional(readOnly = true)
    public List<Vote> getRoomVotes(UUID roomId) {
        return voteRepository.findAllByRoomId(roomId);
    }

    @Transactional(readOnly = true)
    public List<Vote> getUserVotes(UUID userId, UUID roomId) {
        return voteRepository.findAllByUserIdAndRoomId(userId, roomId);
    }

    @Transactional(readOnly = true)
    public List<ExcludedMovie> getExcludedMovies(UUID roomId) {
        return excludedMovieRepository.findAllByRoomId(roomId);
    }

    @Transactional
    public ExcludedMovie excludeMovie(
            UUID userId,
            UUID roomId,
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
            UUID userId,
            UUID roomId,
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

    private void ensureVotingIsOpen(UUID roomId) {
        if (votingResultRepository.existsByRoomId(roomId)) {
            throw new IllegalStateException(
                    "Voting has already finished for this room"
            );
        }
    }

    @Transactional
    public VotingResult finishVoting(UUID roomId) {
        ensureVotingIsOpen(roomId);

        Set<Long> excludedMovieIds = excludedMovieRepository
                .findAllByRoomId(roomId)
                .stream()
                .map(ExcludedMovie::getMovieId)
                .collect(Collectors.toSet());

        Map<Long, Integer> voteCounts = new HashMap<>();

        voteRepository.findAllByRoomId(roomId).stream()
                .filter(vote -> !excludedMovieIds.contains(vote.getMovieId()))
                .forEach(vote -> voteCounts.merge(
                        vote.getMovieId(),
                        1,
                        Integer::sum
                ));

        VotingResult result = new VotingResult();
        result.setRoomId(roomId);

        if (voteCounts.isEmpty()) {
            result.setWinnerMovieIds(new ArrayList<>());
            result.setVoteCount(0);
            return saveResultAndPublishEvent(result);
        }

        int maxVotes = voteCounts.values().stream()
                .max(Integer::compareTo)
                .orElseThrow();

        List<Long> winnerMovieIds = voteCounts.entrySet().stream()
                .filter(entry -> entry.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        result.setWinnerMovieIds(winnerMovieIds);
        result.setVoteCount(maxVotes);

        return saveResultAndPublishEvent(result);
    }

    private VotingResult saveResultAndPublishEvent(VotingResult result) {
        VotingResult savedResult = votingResultRepository.save(result);

        eventPublisher.publishEvent(new VotingResultsReadyEvent(
                savedResult.getRoomId(),
                savedResult.getWinnerMovieIds(),
                savedResult.getVoteCount()
        ));

        return savedResult;
    }

}
