
package org.spring.canapa.backend.voting.service;

import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.movie.repository.MovieRepository;
import org.spring.canapa.backend.notification.event.VotingResultsReadyEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VotingService {

    private final VoteRepository voteRepository;
    private final VotingResultRepository votingResultRepository;
    private final ExcludedMovieRepository excludedMovieRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final ApplicationEventPublisher eventPublisher;

    public VotingService(
        VoteRepository voteRepository,
        VotingResultRepository votingResultRepository,
        ExcludedMovieRepository excludedMovieRepository,
        RoomRepository roomRepository,
        UserRepository userRepository,
        MovieRepository movieRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.voteRepository = voteRepository;
        this.votingResultRepository = votingResultRepository;
        this.excludedMovieRepository = excludedMovieRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Vote addVote(UUID userId, UUID roomId, Long movieId) {
        ensureVotingIsOpen(roomId);

        if (excludedMovieRepository.existsByRoom_IdAndMovie_Id(
            roomId, movieId)) {
            throw new IllegalStateException(
                "Cannot vote for an excluded movie"
            );
        }

        User user = getUser(userId);
        Room room = getRoom(roomId);
        Movie movie = getMovie(movieId);

        Vote vote = voteRepository
            .findByUser_IdAndRoom_IdAndMovie_Id(
                userId, roomId, movieId
            )
            .orElseGet(() -> new Vote(user, room, movie));

        return voteRepository.save(vote);
    }

    @Transactional
    public void removeVote(UUID userId, UUID roomId, Long movieId) {
        ensureVotingIsOpen(roomId);

        voteRepository.deleteByUser_IdAndRoom_IdAndMovie_Id(
            userId, roomId, movieId
        );
    }

    @Transactional(readOnly = true)
    public List<Vote> getRoomVotes(UUID roomId) {
        return voteRepository.findAllByRoom_Id(roomId);
    }

    @Transactional(readOnly = true)
    public List<Vote> getUserVotes(UUID userId, UUID roomId) {
        return voteRepository.findAllByUser_IdAndRoom_Id(userId, roomId);
    }

    @Transactional(readOnly = true)
    public List<ExcludedMovie> getExcludedMovies(UUID roomId) {
        return excludedMovieRepository.findAllByRoom_Id(roomId);
    }

    @Transactional
    public ExcludedMovie excludeMovie(
        UUID userId,
        UUID roomId,
        Long movieId
    ) {
        ensureVotingIsOpen(roomId);

        return excludedMovieRepository
            .findByRoom_IdAndMovie_Id(roomId, movieId)
            .orElseGet(() -> {
                User user = getUser(userId);
                Room room = getRoom(roomId);
                Movie movie = getMovie(movieId);

                return excludedMovieRepository.save(
                    new ExcludedMovie(room, movie, user)
                );
            });
    }

    @Transactional
    public void restoreMovie(UUID userId, UUID roomId, Long movieId) {
        ensureVotingIsOpen(roomId);

        ExcludedMovie excluded = excludedMovieRepository
            .findByRoom_IdAndMovie_Id(roomId, movieId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Excluded movie not found"
            ));

        if (!excluded.getExcludedBy().getId().equals(userId)) {
            throw new IllegalStateException(
                "Only the user who excluded this movie can restore it"
            );
        }

        excludedMovieRepository.delete(excluded);
    }

    @Transactional(readOnly = true)
    public void ensureVotingIsOpen(UUID roomId) {
        if (votingResultRepository.existsByRoom_Id(roomId)) {
            throw new IllegalStateException(
                "Voting has already finished for room: " + roomId
            );
        }
    }

    @Transactional
    public VotingResult finishVoting(UUID roomId) {
        Room room = roomRepository.findByIdForUpdate(roomId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Room not found: " + roomId
            ));

        if (votingResultRepository.existsByRoom_Id(roomId)) {
            throw new IllegalStateException(
                "Voting has already finished for room: " + roomId
            );
        }

        List<Vote> roomVotes = voteRepository.findAllByRoom_Id(roomId);
        Set<UUID> voterIds = roomVotes.stream()
            .map(vote -> vote.getUser().getId())
            .collect(Collectors.toSet());
        Set<UUID> requiredVoterIds = new HashSet<>(
            room.getParticipants().stream()
                .map(User::getId)
                .collect(Collectors.toSet())
        );
        requiredVoterIds.add(room.getCreator().getId());

        long missingVoteCount = requiredVoterIds.stream()
            .filter(participantId -> !voterIds.contains(participantId))
            .count();

        if (missingVoteCount > 0) {
            throw new IllegalStateException(
                "Cannot finish voting until all room participants have voted. "
                    + "Missing votes: " + missingVoteCount
            );
        }

        List<Long> excludedMovieIds = excludedMovieRepository
            .findAllByRoom_Id(roomId)
            .stream()
            .map(excluded -> excluded.getMovie().getId())
            .toList();

        Map<Long, Long> votesByMovie = roomVotes.stream()
            .filter(vote ->
                !excludedMovieIds.contains(vote.getMovie().getId())
            )
            .collect(Collectors.groupingBy(
                vote -> vote.getMovie().getId(),
                Collectors.counting()
            ));

        if (votesByMovie.isEmpty()) {
            return saveResultAndPublishEvent(
                new VotingResult(room, List.of(), 0)
            );
        }

        long maxVotes = votesByMovie.values()
            .stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0);

        List<Long> winnerIds = votesByMovie.entrySet()
            .stream()
            .filter(entry -> entry.getValue() == maxVotes)
            .map(Map.Entry::getKey)
            .sorted()
            .toList();

        List<Movie> winners = movieRepository.findAllById(winnerIds);

        VotingResult result = new VotingResult(
            room,
            winners,
            Math.toIntExact(maxVotes)
        );

        return saveResultAndPublishEvent(result);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "User not found: " + userId
            ));
    }

    private Room getRoom(UUID roomId) {
        return roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Room not found: " + roomId
            ));
    }

    private Movie getMovie(Long movieId) {
        return movieRepository.findById(movieId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Movie not found: " + movieId
            ));
    }

    private VotingResult saveResultAndPublishEvent(VotingResult result) {
        result.getRoom().markVotingFinished();
        roomRepository.save(result.getRoom());
        VotingResult savedResult = votingResultRepository.save(result);

        eventPublisher.publishEvent(new VotingResultsReadyEvent(
            savedResult.getRoom().getId(),
            savedResult.getWinners().stream()
                .map(Movie::getId)
                .sorted()
                .toList(),
            savedResult.getVoteCount()
        ));

        return savedResult;
    }
}
