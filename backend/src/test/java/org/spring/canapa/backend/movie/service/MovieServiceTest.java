package org.spring.canapa.backend.movie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.movie.provider.OmdbMovieProvider;
import org.spring.canapa.backend.movie.provider.TmdbMovieProvider;
import org.spring.canapa.backend.movie.repository.MovieRepository;
import org.spring.canapa.backend.movie.service.exception.MovieNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private TmdbMovieProvider tmdbProvider;

    @Mock
    private OmdbMovieProvider omdbProvider;

    private MovieService movieService;

    @BeforeEach
    void setUp() {
        movieService = new MovieService(
            movieRepository,
            tmdbProvider,
            omdbProvider
        );
    }

    @Test
    void searchesLocalMoviesWhenProvidersAreNotConfigured() {
        Movie movie = new Movie();
        movie.setTitle("Inception");
        movie.setRating(8.8);

        Page<Movie> page = new PageImpl<>(List.of(movie));

        when(tmdbProvider.isConfigured()).thenReturn(false);
        when(omdbProvider.isConfigured()).thenReturn(false);
        when(movieRepository.findByTitleContainingIgnoreCase(
            eq("Inception"),
            any(PageRequest.class)
        )).thenReturn(page);

        var response = movieService.searchMovies("Inception", 1, 20, "en-US");

        assertEquals(1, response.results().size());
        assertEquals("Inception", response.results().getFirst().title());
        verify(movieRepository).findByTitleContainingIgnoreCase(
            eq("Inception"),
            any(PageRequest.class)
        );
    }

    @Test
    void getsMovieByTmdbIdFromProvider() {
        Movie movie = new Movie();
        movie.setTmdbId(27205L);
        movie.setTitle("Inception");

        when(movieRepository.findByTmdbId(27205L))
            .thenReturn(Optional.empty());

        when(tmdbProvider.isConfigured()).thenReturn(true);

        when(tmdbProvider.getMovieDetails(27205L))
            .thenReturn(Optional.of(movie));

        when(movieRepository.save(any(Movie.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = movieService.getMovieByTmdbId(27205L);

        assertEquals(27205L, response.tmdbId());
        assertEquals("Inception", response.title());
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void throwsWhenMovieDoesNotExist() {
        when(movieRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
            MovieNotFoundException.class,
            () -> movieService.getMovieById(999L)
        );
    }

    @Test
    void validatesSearchPagination() {
        assertThrows(
            IllegalArgumentException.class,
            () -> movieService.searchMovies("Inception", 0, 20, "en-US")
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> movieService.searchMovies("Inception", 1, 101, "en-US")
        );
    }
}
