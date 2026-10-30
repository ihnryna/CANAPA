import { useCallback, useEffect, useRef, useState } from 'react'
import {
  getPopularMovies,
  searchMovies,
  type Movie,
  type MovieSearchResponse,
} from '../api/movies'
import './MovieBrowser.css'

interface MovieBrowserProps {
  initialLimit?: number
  onSelectMovie?: (movie: Movie) => void
  selectedMovieIds?: number[]
}

type BrowserState = 'popular' | 'search'

function formatRuntime(runtime: number | null): string {
  if (!runtime || runtime <= 0) return 'Тривалість невідома'

  const hours = Math.floor(runtime / 60)
  const minutes = runtime % 60

  if (hours === 0) return `${minutes} хв`
  if (minutes === 0) return `${hours} год`
  return `${hours} год ${minutes} хв`
}

function MoviePoster({ movie }: { movie: Movie }) {
  const [failed, setFailed] = useState(false)

  const posterUrl = movie.posterUrl?.trim()
  const hasPoster = Boolean(posterUrl) && !failed

  return (
    <div className="browser-poster">
      {hasPoster ? (
        <img
          src={posterUrl!}
          alt={`Постер фільму «${movie.title}»`}
          loading="lazy"
          onError={() => setFailed(true)}
        />
      ) : (
        <div className="browser-poster-placeholder">
          <span aria-hidden="true">🎬</span>
          <span>{movie.title}</span>
        </div>
      )}

      {movie.rating != null && (
        <span className="browser-rating">
          ★ {movie.rating.toFixed(1)}
        </span>
      )}
    </div>
  )
}

function MovieCard({
                     movie,
                     onSelectMovie,
                     selected,
                   }: {
  movie: Movie
  onSelectMovie?: (movie: Movie) => void
  selected: boolean
}) {
  const [expanded, setExpanded] = useState(false)

  return (
    <article className="browser-movie-card">
      <MoviePoster movie={movie}/>

      <div className="browser-movie-content">
        <h3>{movie.title}</h3>

        <p className="browser-movie-meta">
          {movie.releaseYear ?? 'Рік невідомий'}
          {' · '}
          {formatRuntime(movie.runtime)}
        </p>

        {movie.genres?.length > 0 && (
          <div className="browser-movie-genres">
            {movie.genres.map((genre) => (
              <span key={genre}>{genre}</span>
            ))}
          </div>
        )}

        <p className={`browser-movie-description ${expanded ? 'expanded' : ''}`}>
          {movie.description || 'Опис цього фільму поки недоступний.'}
        </p>

        {movie.description && movie.description.length > 130 && (
          <button
            className="browser-more-button"
            type="button"
            onClick={() => setExpanded((current) => !current)}
            aria-expanded={expanded}
          >
            {expanded ? 'Менше' : 'Читати більше'}
          </button>
        )}

        {movie.director && (
          <p className="browser-movie-detail">
            <strong>Режисер:</strong> {movie.director}
          </p>
        )}

        {movie.actors && (
          <p className="browser-movie-detail">
            <strong>У ролях:</strong> {movie.actors}
          </p>
        )}

        {onSelectMovie && (
          <button
            type="button"
            className={`btn ${selected ? 'secondary' : 'primary'} browser-select-button`}
            onClick={() => onSelectMovie(movie)}
            aria-pressed={selected}
          >
            {selected ? 'Обрано ✓' : 'Обрати фільм'}
          </button>
        )}
      </div>
    </article>
  )
}

export function MovieBrowser({
                               initialLimit = 24,
                               onSelectMovie,
                               selectedMovieIds = [],
                             }: MovieBrowserProps) {
  const [query, setQuery] = useState('')
  const [draftQuery, setDraftQuery] = useState('')
  const [state, setState] = useState<BrowserState>('popular')
  const [movies, setMovies] = useState<Movie[]>([])
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [totalResults, setTotalResults] = useState(0)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState('')

  const requestSequence = useRef(0)

  const loadMovies = useCallback(
    async (searchTerm: string, nextPage: number, append: boolean) => {
      const requestId = ++requestSequence.current

      if (append) setLoadingMore(true)
      else setLoading(true)

      setError('')

      try {
        if (searchTerm.trim()) {
          const response: MovieSearchResponse = await searchMovies(
            searchTerm.trim(),
            nextPage,
            initialLimit,
            'uk-UA',
          )

          if (requestId !== requestSequence.current) return

          setMovies((current) =>
            append ? [...current, ...response.results] : response.results,
          )
          setPage(response.page)
          setTotalPages(response.totalPages)
          setTotalResults(response.totalResults)
          setState('search')
        } else {
          const results = await getPopularMovies(initialLimit)

          if (requestId !== requestSequence.current) return

          setMovies(results)
          setPage(1)
          setTotalPages(1)
          setTotalResults(results.length)
          setState('popular')
        }
      } catch {
        if (requestId === requestSequence.current) {
          setError(
            'Не вдалося завантажити фільми. Перевірте з’єднання та спробуйте ще раз.',
          )
        }
      } finally {
        if (requestId === requestSequence.current) {
          setLoading(false)
          setLoadingMore(false)
        }
      }
    },
    [initialLimit],
  )

  useEffect(() => {
    void loadMovies('', 1, false)

    return () => {
      requestSequence.current++
    }
  }, [loadMovies])

  function handleSearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const trimmed = draftQuery.trim()
    setQuery(trimmed)
    void loadMovies(trimmed, 1, false)
  }

  function clearSearch() {
    setDraftQuery('')
    setQuery('')
    void loadMovies('', 1, false)
  }

  function loadNextPage() {
    if (loadingMore || loading || page >= totalPages || !query) return
    void loadMovies(query, page + 1, true)
  }

  return (
    <section className="movie-browser">
      <div className="movie-browser-heading">
        <div>
          <span className="eyebrow">MOVIE COLLECTION</span>
          <h2>Знайдіть свій фільм</h2>
          <p>
            Переглядайте популярні фільми або шукайте щось конкретне.
          </p>
        </div>
      </div>

      <form className="browser-search-form" onSubmit={handleSearch}>
        <label htmlFor="movie-search-input">Назва фільму</label>

        <div className="browser-search-row">
          <input
            id="movie-search-input"
            type="search"
            value={draftQuery}
            onChange={(event) => setDraftQuery(event.target.value)}
            placeholder="Наприклад, Інтерстеллар..."
            maxLength={150}
          />

          <button className="btn primary" type="submit" disabled={loading}>
            Знайти
          </button>

          {(query || draftQuery) && (
            <button
              className="btn secondary"
              type="button"
              onClick={clearSearch}
            >
              Очистити
            </button>
          )}
        </div>
      </form>

      <div className="browser-results-heading">
        <h3>{state === 'search' ? 'Результати пошуку' : 'Популярні фільми'}</h3>
        {!loading && !error && (
          <span className="browser-result-count">
            {totalResults} {totalResults === 1 ? 'фільм' : 'фільмів'}
          </span>
        )}
      </div>

      {error && (
        <div className="browser-error" role="alert">
          <p>{error}</p>
          <button
            type="button"
            className="btn secondary"
            onClick={() => void loadMovies(query, 1, false)}
          >
            Спробувати ще раз
          </button>
        </div>
      )}

      {loading && (
        <div className="browser-loading" aria-live="polite">
          <span className="spinner"/>
          <p>Завантажуємо фільми...</p>
        </div>
      )}

      {!loading && !error && movies.length === 0 && (
        <div className="browser-empty">
          <span aria-hidden="true">🎞️</span>
          <h3>Фільмів не знайдено</h3>
          <p>Спробуйте іншу назву або перегляньте популярні фільми.</p>
          {query && (
            <button
              type="button"
              className="btn secondary"
              onClick={clearSearch}
            >
              Популярні фільми
            </button>
          )}
        </div>
      )}

      {!error && movies.length > 0 && (
        <div className="browser-movie-grid">
          {movies.map((movie) => (
            <MovieCard
              key={movie.id}
              movie={movie}
              onSelectMovie={onSelectMovie}
              selected={selectedMovieIds.includes(movie.id)}
            />
          ))}
        </div>
      )}

      {!loading && !error && query && page < totalPages && (
        <div className="browser-load-more">
          <button
            type="button"
            className="btn secondary"
            onClick={loadNextPage}
            disabled={loadingMore}
          >
            {loadingMore ? 'Завантажуємо...' : 'Завантажити ще'}
          </button>
          <p>
            Сторінка {page} із {totalPages}
          </p>
        </div>
      )}
    </section>
  )
}
