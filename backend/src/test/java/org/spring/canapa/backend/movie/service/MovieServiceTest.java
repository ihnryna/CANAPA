package org.spring.canapa.backend.movie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.canapa.backend.movie.dto.MovieDiscoveryCriteria;
import org.spring.canapa.backend.movie.dto.MovieSearchResult;
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
import java.util.Set;

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

    @Test
    void discoversWithTmdbAndUsesOmdbExpandedDescription() {
        var criteria = new MovieDiscoveryCriteria(
            Set.of("Science Fiction"),
            Set.of("Horror"),
            7.0,
            180,
            Set.of("en")
        );

        when(tmdbProvider.isConfigured()).thenReturn(true);
        when(omdbProvider.isConfigured()).thenReturn(true);
        when(tmdbProvider.discoverMovies(criteria, 1, "uk-UA"))
            .thenReturn(new MovieSearchResult(
                1,
                1,
                1,
                List.of(new MovieSearchResult.MovieSearchItem(
                    157336L,
                    "Interstellar",
                    "Short TMDB overview",
                    null,
                    "2014-11-05",
                    8.5,
                    "en",
                    List.of("Science Fiction")
                ))
            ));
        when(movieRepository.findByTmdbId(157336L))
            .thenReturn(Optional.empty());
        when(movieRepository.save(any(Movie.class)))
            .thenAnswer(invocation -> {
                Movie movie = invocation.getArgument(0);
                if (movie.getId() == null) movie.setId(1L);
                return movie;
            });

        Movie tmdbDetails = new Movie();
        tmdbDetails.setTmdbId(157336L);
        tmdbDetails.setImdbId("tt0816692");
        tmdbDetails.setTitle("Interstellar");
        tmdbDetails.setDescription("Longer TMDB overview");
        tmdbDetails.setRuntime(169);
        when(tmdbProvider.getMovieDetails(157336L))
            .thenReturn(Optional.of(tmdbDetails));

        Movie omdbDetails = new Movie();
        omdbDetails.setImdbId("tt0816692");
        omdbDetails.setTitle("Interstellar");
        omdbDetails.setDescription("Expanded OMDb plot");
        omdbDetails.setDirector("Christopher Nolan");
        when(omdbProvider.getMovieDetailsByImdbId("tt0816692"))
            .thenReturn(Optional.of(omdbDetails));

        var result = movieService.discoverMovies(criteria, 20);

        assertEquals(1, result.size());
        assertEquals("Expanded OMDb plot", result.getFirst().description());
        assertEquals("Christopher Nolan", result.getFirst().director());
    }

    @Test
    void fallsBackToOmdbTitleLookupWhenTmdbHasNoImdbId() {
        var criteria = new MovieDiscoveryCriteria(
            Set.of("Drama"), Set.of(), 6.0, 180, Set.of("en")
        );
        when(tmdbProvider.isConfigured()).thenReturn(true);
        when(omdbProvider.isConfigured()).thenReturn(true);
        when(tmdbProvider.discoverMovies(criteria, 1, "uk-UA"))
            .thenReturn(new MovieSearchResult(
                1, 1, 1,
                List.of(new MovieSearchResult.MovieSearchItem(
                    99L, "New Movie", "TMDB overview", null,
                    "2026-04-01", 7.0, "en", List.of("Drama")
                ))
            ));
        when(movieRepository.findByTmdbId(99L)).thenReturn(Optional.empty());
        when(movieRepository.save(any(Movie.class)))
            .thenAnswer(invocation -> {
                Movie movie = invocation.getArgument(0);
                if (movie.getId() == null) movie.setId(2L);
                return movie;
            });

        Movie tmdbDetails = new Movie();
        tmdbDetails.setTmdbId(99L);
        tmdbDetails.setTitle("New Movie");
        tmdbDetails.setReleaseYear(2026);
        tmdbDetails.setRuntime(120);
        when(tmdbProvider.getMovieDetails(99L))
            .thenReturn(Optional.of(tmdbDetails));

        Movie omdbDetails = new Movie();
        omdbDetails.setImdbId("tt9999999");
        omdbDetails.setTitle("New Movie");
        omdbDetails.setDescription("Expanded title-match plot");
        when(omdbProvider.getMovieDetailsByTitle("New Movie", 2026))
            .thenReturn(Optional.of(omdbDetails));

        var result = movieService.discoverMovies(criteria, 10);

        assertEquals("Expanded title-match plot", result.getFirst().description());
        assertEquals("tt9999999", result.getFirst().imdbId());
        verify(omdbProvider).getMovieDetailsByTitle("New Movie", 2026);
    }

    @Test
    void requiresAtLeastOneLikedGenreFromEveryUserGroup() {
        var criteria = new MovieDiscoveryCriteria(
            Set.of("Comedy", "Science Fiction", "Drama", "Horror"),
            Set.of(),
            null,
            null,
            Set.of("en"),
            List.of(
                Set.of("Comedy", "Science Fiction"),
                Set.of("Drama", "Horror")
            )
        );

        when(tmdbProvider.isConfigured()).thenReturn(true);
        when(tmdbProvider.discoverMovies(criteria, 1, "uk-UA"))
            .thenReturn(new MovieSearchResult(
                1, 1, 4, List.of(
                    searchItem(1L, "Only first user", List.of("Comedy")),
                    searchItem(2L, "Comedy Drama", List.of("Comedy", "Drama")),
                    searchItem(3L, "Sci-Fi Horror", List.of("Science Fiction", "Horror")),
                    searchItem(4L, "Only second user", List.of("Drama"))
                )
            ));
        when(movieRepository.findByTmdbId(any(Long.class)))
            .thenReturn(Optional.empty());
        when(movieRepository.save(any(Movie.class)))
            .thenAnswer(invocation -> {
                Movie movie = invocation.getArgument(0);
                movie.setId(movie.getTmdbId());
                return movie;
            });
        when(tmdbProvider.getMovieDetails(any(Long.class)))
            .thenReturn(Optional.empty());

        var result = movieService.discoverMovies(criteria, 10);

        assertEquals(2, result.size());
        assertEquals(
            Set.of("Comedy Drama", "Sci-Fi Horror"),
            result.stream()
                .map(movie -> movie.title())
                .collect(java.util.stream.Collectors.toSet())
        );
        verify(tmdbProvider).discoverMovies(criteria, 1, "uk-UA");
    }

    @Test
    void discoversWithoutOriginalLanguageFilterWhenNoneWasSelected() {
        var criteria = new MovieDiscoveryCriteria(
            Set.of("Drama"), Set.of(), null, null, Set.of()
        );

        when(tmdbProvider.isConfigured()).thenReturn(true);
        when(tmdbProvider.discoverMovies(criteria, 1, "uk-UA"))
            .thenReturn(new MovieSearchResult(1, 0, 0, List.of()));

        var result = movieService.discoverMovies(criteria, 10);

        assertEquals(List.of(), result);
        verify(tmdbProvider).discoverMovies(criteria, 1, "uk-UA");
    }

    private MovieSearchResult.MovieSearchItem searchItem(
        long id,
        String title,
        List<String> genres
    ) {
        return new MovieSearchResult.MovieSearchItem(
            id,
            title,
            null,
            null,
            "2025-01-01",
            7.5,
            "en",
            genres
        );
    }
}
