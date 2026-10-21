import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getPopularMovies, searchMovies, type Movie } from '../api/movies'
import {
  addVote,
  excludeMovie,
  finishVoting,
  getExcludedMovies,
  getRoomVotes,
  getVotingResult,
  removeVote,
  restoreMovie,
  type ExcludedMovie,
  type VotingResult,
  type Vote,
} from '../api/voting'
import { useCurrentUser } from '../auth/current-user-context'
import { isUuid } from '../auth/user-id'
import { AppHeader } from '../components/AppHeader'
import { Icon } from '../components/Icon'
import { PageFooter } from '../components/PageFooter'
import { useRoomPolling } from '../hooks/useRoomPolling'
// TEMP: FOR DEVELOPING
import { MOCK_MOVIES } from '../mocks/votingMock'

const USE_MOCK_VOTING = true

//

function formatRating(rating: number | null): string {
  return rating == null ? 'Немає оцінки' : rating.toFixed(1)
}

// TODO: Restrict access to VotingPage so it can be opened only when the room
// status is VOTING. For now, allow direct access during frontend development
// to make it possible to preview and test the page before voting starts.
export function VotingPage() {
  const { roomId } = useParams()
  const validRoomId =
    roomId !== undefined && isUuid(roomId) ? roomId : undefined

  const { user } = useCurrentUser()
  const {
    room,
    loading: roomLoading,
    error: roomError,
    refresh: refreshRoom,
  } = useRoomPolling(validRoomId)

  const [movies, setMovies] = useState<Movie[]>([])
  const [votes, setVotes] = useState<Vote[]>([])
  const [excludedMovies, setExcludedMovies] = useState<ExcludedMovie[]>([])
  const [result, setResult] = useState<VotingResult | null>(null)

  const [loadingMovies, setLoadingMovies] = useState(true)
  const [pendingMovieId, setPendingMovieId] = useState<number | null>(null)
  const [finishing, setFinishing] = useState(false)
  const [query, setQuery] = useState('')
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  // TEMP: FOR DEVELOPING
  const [mockFinished, setMockFinished] = useState(false)
  //

  const organizer = room?.creator.id === user?.id
  // TEMP: FOR DEVELOPING
  //const isVoting = room?.status === 'VOTING'
  //const isFinished = room?.status === 'FINISHED'
  const isVoting = USE_MOCK_VOTING
    ? !mockFinished
    : room?.status === 'VOTING'

  const isFinished = USE_MOCK_VOTING
    ? mockFinished
    : room?.status === 'FINISHED'
  //

  const ownVotes = useMemo(
    () => new Set(
      votes
        .filter((vote) => vote.userId === user?.id)
        .map((vote) => vote.movieId),
    ),
    [votes, user?.id],
  )

  const excludedByMovieId = useMemo(
    () => new Map(
      excludedMovies.map((movie) => [movie.movieId, movie]),
    ),
    [excludedMovies],
  )

  const voteCounts = useMemo(() => {
    const counts = new Map<number, number>()

    for (const vote of votes) {
      counts.set(vote.movieId, (counts.get(vote.movieId) ?? 0) + 1)
    }

    return counts
  }, [votes])

  const movieById = useMemo(
    () => new Map(movies.map((movie) => [movie.id, movie])),
    [movies],
  )

  const loadData = useCallback(async () => {
    await Promise.resolve()
    // TEMP: FOR DEVELOPING
    if (USE_MOCK_VOTING) {
      setMovies(MOCK_MOVIES)
      setLoadingMovies(false)
      setError(null)
      return
    }
    //

    if (validRoomId === undefined || user === null || !isUuid(user.id)) {
      setLoadingMovies(false)
      return
    }

    setError(null)
    setLoadingMovies(true)

    try {
      const [loadedMovies, loadedVotes, loadedExcluded] =
        await Promise.all([
          getPopularMovies(40),
          getRoomVotes(validRoomId),
          getExcludedMovies(validRoomId),
        ])

      setMovies(loadedMovies)
      setVotes(loadedVotes)
      setExcludedMovies(loadedExcluded)

      if (room?.status === 'FINISHED') {
        setResult(await getVotingResult(validRoomId))
      } else {
        setResult(null)
      }
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : 'Не вдалося завантажити дані голосування.',
      )
    } finally {
      setLoadingMovies(false)
    }
  }, [validRoomId, user, room])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void loadData()
    }, 0)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [loadData])

  async function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const normalizedQuery = query.trim()

    if (!normalizedQuery) {
      await loadData()
      return
    }

    // TEMP: FOR DEVELOPING
    if (USE_MOCK_VOTING) {
      const normalized = normalizedQuery.toLocaleLowerCase()

      const filteredMovies = MOCK_MOVIES.filter((movie) => {
        const searchableText = [
          movie.title,
          movie.description ?? '',
          ...movie.genres,
          String(movie.releaseYear ?? ''),
        ]
          .join(' ')
          .toLocaleLowerCase()

        return searchableText.includes(normalized)
      })

      setMovies(filteredMovies)
      setNotice(
        filteredMovies.length === 0
          ? 'За цим запитом фільмів не знайдено.'
          : null,
      )
      setError(null)
      return
    }
    //

    setSearching(true)
    setError(null)

    try {
      const response = await searchMovies(normalizedQuery)
      setMovies(response.results)

      if (response.results.length === 0) {
        setNotice('За цим запитом фільмів не знайдено.')
      } else {
        setNotice(null)
      }
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : 'Не вдалося знайти фільми.',
      )
    } finally {
      setSearching(false)
    }
  }

  async function handleVote(movieId: number) {
    if (
      validRoomId === undefined ||
      user === null ||
      !isVoting ||
      excludedByMovieId.has(movieId)
    ) {
      return
    }
    // TEMP: FOR DEVELOPING
    if (USE_MOCK_VOTING) {
      if (ownVotes.has(movieId)) {
        setVotes((currentVotes) =>
          currentVotes.filter(
            (vote) =>
              !(vote.userId === user.id && vote.movieId === movieId),
          ),
        )
        setNotice('Голос скасовано.')
      } else {
        setVotes((currentVotes) => [
          ...currentVotes,
          {
            id: Date.now(),
            userId: user.id,
            roomId: validRoomId,
            movieId,
            createdAt: new Date().toISOString(),
          },
        ])
        setNotice('Ваш голос зараховано.')
      }

      setError(null)
      return
    }
    //

    setPendingMovieId(movieId)
    setError(null)
    setNotice(null)

    try {
      if (ownVotes.has(movieId)) {
        await removeVote(validRoomId, movieId, user.id)
        setNotice('Голос скасовано.')
      } else {
        await addVote(validRoomId, user.id, movieId)
        setNotice('Ваш голос зараховано.')
      }

      await loadData()
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : 'Не вдалося оновити голос.',
      )
    } finally {
      setPendingMovieId(null)
    }
  }

  async function handleExclude(movieId: number) {
    if (validRoomId === undefined || user === null || !isVoting) {
      return
    }

    // TEMP: FOR DEVELOPING
    if (USE_MOCK_VOTING) {
      setExcludedMovies((currentMovies) => [
        ...currentMovies,
        {
          id: Date.now(),
          roomId: validRoomId,
          movieId,
          excludedByUserId: user.id,
          excludedAt: new Date().toISOString(),
        },
      ])
      setNotice('Фільм позначено як уже переглянутий.')
      setError(null)
      return
    }
    //

    setPendingMovieId(movieId)
    setError(null)
    setNotice(null)

    try {
      await excludeMovie(validRoomId, user.id, movieId)
      setNotice('Фільм позначено як уже переглянутий.')
      await loadData()
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : 'Не вдалося виключити фільм.',
      )
    } finally {
      setPendingMovieId(null)
    }
  }

  async function handleRestore(movieId: number) {
    if (validRoomId === undefined || user === null || !isVoting) {
      return
    }

    // TEMP: FOR DEVELOPING
    if (USE_MOCK_VOTING) {
      setExcludedMovies((currentMovies) =>
        currentMovies.filter(
          (movie) =>
            !(
              movie.movieId === movieId &&
              movie.excludedByUserId === user.id
            ),
        ),
      )
      setNotice('Фільм повернуто до голосування.')
      setError(null)
      return
    }
    //

    setPendingMovieId(movieId)
    setError(null)
    setNotice(null)

    try {
      await restoreMovie(validRoomId, movieId, user.id)
      setNotice('Фільм повернуто до голосування.')
      await loadData()
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : 'Не вдалося відновити фільм.',
      )
    } finally {
      setPendingMovieId(null)
    }
  }

  async function handleFinishVoting() {
    if (validRoomId === undefined || !organizer || !isVoting) {
      return
    }

    const confirmed = window.confirm(
      'Завершити голосування? Після цього змінити голоси не вийде.',
    )

    if (!confirmed) return

    // TEMP: FOR DEVELOPING
    if (USE_MOCK_VOTING) {
      const availableMovieIds = MOCK_MOVIES
        .filter((movie) => !excludedByMovieId.has(movie.id))
        .map((movie) => movie.id)

      const maxVotes = Math.max(
        0,
        ...availableMovieIds.map((id) => voteCounts.get(id) ?? 0),
      )

      const winnerMovieIds =
        maxVotes === 0
          ? []
          : availableMovieIds.filter(
            (id) => (voteCounts.get(id) ?? 0) === maxVotes,
          )

      setResult({
        id: Date.now(),
        roomId: validRoomId,
        winnerMovieIds,
        voteCount: votes.length,
        finishedAt: new Date().toISOString(),
      })

      setMockFinished(true)
      setNotice('Тестове голосування завершено.')
      return
    }
    //

    setFinishing(true)
    setError(null)
    setNotice(null)

    try {
      const votingResult = await finishVoting(validRoomId)

      await refreshRoom()
      await loadData()

      // Зберігаємо результат після повторного завантаження даних.
      setResult(votingResult)
      setNotice('Голосування завершено.')
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : 'Не вдалося завершити голосування.',
      )
    } finally {
      setFinishing(false)
    }
  }

  if (validRoomId === undefined) {
    return (
      <div className="app-shell">
        <AppHeader/>
        <main className="centered-state">
          <h1>Некоректний ID кімнати</h1>
          <Link className="btn primary" to="/rooms">
            До моїх кімнат
          </Link>
        </main>
        <PageFooter/>
      </div>
    )
  }

  if (roomLoading && room === null) {
    return (
      <div className="app-shell">
        <AppHeader/>
        <main className="centered-state" aria-busy="true">
          <span className="spinner"/>
          <h1>Завантажуємо голосування…</h1>
        </main>
        <PageFooter/>
      </div>
    )
  }

  if (room === null) {
    return (
      <div className="app-shell">
        <AppHeader/>
        <main className="centered-state">
          <h1>Не вдалося відкрити кімнату</h1>
          <p role="alert">{roomError ?? 'Кімнату не знайдено.'}</p>
          <Link className="btn primary" to="/rooms">
            До моїх кімнат
          </Link>
        </main>
        <PageFooter/>
      </div>
    )
  }

  if (user === null || !isUuid(user.id)) {
    return (
      <div className="app-shell">
        <AppHeader/>
        <main className="centered-state">
          <h1>Спочатку оберіть профіль</h1>
          <p>Для голосування потрібен профіль учасника CANAPA.</p>
          <Link className="btn primary" to="/">
            Обрати профіль
          </Link>
        </main>
        <PageFooter/>
      </div>
    )
  }

  return (
    <div className="app-shell">
      <AppHeader/>

      <main className="main-content voting-page">
        <Link className="back-link" to={`/rooms/${room.id}`}>
          <Icon name="back" size={16}/>
          Повернутися до кімнати
        </Link>

        <section className="voting-heading">
          <div>
            <div className="eyebrow">ЧАС ОБИРАТИ ФІЛЬМ</div>
            <h1>{room.name}</h1>
            <p>
              Обирайте всі фільми, які хотіли б подивитися разом.
              Свій голос можна скасувати до завершення голосування.
            </p>
          </div>

          <span className={`status-badge status-${room.status.toLowerCase()}`}>
                        {isFinished
                          ? 'Завершено'
                          : isVoting
                            ? 'Голосування триває'
                            : 'Ще не розпочато'}
                    </span>
        </section>

        {error !== null && (
          <div className="voting-alert" role="alert">
            <span>{error}</span>
            <button
              className="text-button"
              onClick={() => void loadData()}
            >
              Спробувати ще раз
            </button>
          </div>
        )}

        {notice !== null && (
          <div className="voting-notice" role="status">
            {notice}
          </div>
        )}

        {!isVoting && !isFinished && (
          <section className="voting-state-panel">
            <h2>Голосування ще не розпочалося</h2>
            <p>
              Коли статус кімнати зміниться на «Голосування триває»,
              тут можна буде обирати фільми.
            </p>
          </section>
        )}

        {isFinished && (
          <section className="voting-state-panel result-panel">
            <div className="eyebrow">РЕЗУЛЬТАТ</div>
            <h2>Голосування завершено</h2>

            {result === null ? (
              <p>Завантажуємо результат голосування…</p>
            ) : result.winnerMovieIds.length === 0 ? (
              <p>Немає фільмів-переможців.</p>
            ) : (
              <>
                <p>
                  {result.winnerMovieIds.length > 1
                    ? 'Нічия! Переможці:'
                    : 'Фільм-переможець:'}
                </p>
                <div className="winner-list">
                  {result.winnerMovieIds.map((id) => {
                    const movie = movieById.get(id)

                    return (
                      <div className="winner-item" key={id}>
                        {movie?.posterUrl ? (
                          <img
                            src={movie.posterUrl}
                            alt=""
                          />
                        ) : (
                          <span className="poster-placeholder">
                                                        <Icon name="film" size={28}/>
                                                    </span>
                        )}
                        <strong>
                          {movie?.title ?? `Фільм #${id}`}
                        </strong>
                      </div>
                    )
                  })}
                </div>
                <p className="subtle-note">
                  Загальна кількість голосів за переможців: {result.voteCount}
                </p>
              </>
            )}
          </section>
        )}

        <section className="voting-toolbar">
          <form className="movie-search" onSubmit={handleSearch}>
            <label htmlFor="movie-search-input">
              Знайти фільм
            </label>
            <div className="movie-search-row">
              <input
                id="movie-search-input"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Наприклад, Interstellar"
                maxLength={150}
              />
              <button
                className="btn primary"
                disabled={searching}
              >
                {searching ? 'Шукаємо…' : 'Знайти'}
              </button>
              {query.trim() !== '' && (
                <button
                  type="button"
                  className="btn secondary"
                  onClick={() => {
                    setQuery('')
                    void loadData()
                  }}
                >
                  Усі фільми
                </button>
              )}
            </div>
          </form>

          <div className="voting-summary">
                        <span>
                            <strong>{ownVotes.size}</strong> ваших голосів
                        </span>
            <span>
                            <strong>{votes.length}</strong> голосів загалом
                        </span>
          </div>
        </section>

        {loadingMovies ? (
          <div className="centered-state compact-state" aria-busy="true">
            <span className="spinner"/>
            <p>Завантажуємо фільми…</p>
          </div>
        ) : movies.length === 0 ? (
          <section className="voting-state-panel">
            <h2>Фільмів поки немає</h2>
            <p>
              Спробуйте пошукати фільм за назвою. Результати пошуку
              можуть бути завантажені з кінопровайдера та збережені
              в базі CANAPA.
            </p>
          </section>
        ) : (
          <section className="movie-grid" aria-label="Фільми для голосування">
            {movies.map((movie) => {
              const excluded = excludedByMovieId.get(movie.id)
              const isOwnVote = ownVotes.has(movie.id)
              const pending = pendingMovieId === movie.id
              const canRestore =
                excluded?.excludedByUserId === user.id

              return (
                <article
                  className={`movie-card${excluded ? ' movie-card-excluded' : ''}${isOwnVote ? ' movie-card-voted' : ''}`}
                  key={movie.id}
                >
                  <div className="movie-poster">
                    {movie.posterUrl ? (
                      <img
                        src={movie.posterUrl}
                        alt={`Постер: ${movie.title}`}
                        loading="lazy"
                      />
                    ) : (
                      <div className="poster-placeholder">
                        <Icon name="film" size={36}/>
                        <span>Постер відсутній</span>
                      </div>
                    )}

                    {isOwnVote && (
                      <span className="movie-vote-badge">
                                                <Icon name="check" size={14}/>
                                                Ваш голос
                                            </span>
                    )}

                    {excluded && (
                      <span className="movie-excluded-badge">
                                                Уже переглянуто
                                            </span>
                    )}
                  </div>

                  <div className="movie-card-content">
                    <div className="movie-card-heading">
                      <h2>{movie.title}</h2>
                      <span className="movie-rating">
                                                ★ {formatRating(movie.rating)}
                                            </span>
                    </div>

                    <p className="movie-meta">
                      {movie.releaseYear ?? 'Рік невідомий'}
                      {movie.runtime ? ` · ${movie.runtime} хв` : ''}
                    </p>

                    {movie.genres.length > 0 && (
                      <div className="movie-genres">
                        {movie.genres.slice(0, 3).map((genre) => (
                          <span key={genre}>{genre}</span>
                        ))}
                      </div>
                    )}

                    <p className="movie-description">
                      {movie.description?.trim()
                        || 'Опис цього фільму поки відсутній.'}
                    </p>

                    <div className="movie-vote-count">
                      {voteCounts.get(movie.id) ?? 0}{' '}
                      {(voteCounts.get(movie.id) ?? 0) === 1
                        ? 'голос'
                        : 'голосів'}
                    </div>

                    {isVoting ? (
                      <>
                        <button
                          className={`btn movie-vote-button${isOwnVote ? ' secondary' : ' primary'}`}
                          disabled={pending || Boolean(excluded)}
                          onClick={() => void handleVote(movie.id)}
                        >
                          {pending
                            ? 'Зачекайте…'
                            : isOwnVote
                              ? 'Скасувати голос'
                              : 'Голосувати'}
                        </button>

                        {excluded ? (
                          canRestore ? (
                            <button
                              className="text-button movie-secondary-action"
                              disabled={pending}
                              onClick={() => void handleRestore(movie.id)}
                            >
                              Повернути до голосування
                            </button>
                          ) : (
                            <p className="movie-locked-note">
                              Виключив інший учасник
                            </p>
                          )
                        ) : (
                          <button
                            className="text-button movie-secondary-action"
                            disabled={pending}
                            onClick={() => void handleExclude(movie.id)}
                          >
                            Уже дивилася
                          </button>
                        )}
                      </>
                    ) : (
                      <p className="movie-locked-note">
                        {isFinished
                          ? 'Голосування завершено'
                          : 'Голосування ще не розпочалося'}
                      </p>
                    )}
                  </div>
                </article>
              )
            })}
          </section>
        )}

        {organizer && isVoting && (
          <section className="finish-voting-panel">
            <div>
              <h2>Усі вже визначилися?</h2>
              <p>
                Завершення зафіксує результат. Перевірте голоси
                учасників перед продовженням.
              </p>
            </div>
            <button
              className="btn primary"
              disabled={finishing}
              onClick={() => void handleFinishVoting()}
            >
              {finishing ? 'Завершуємо…' : 'Завершити голосування'}
            </button>
          </section>
        )}

        <div className="voting-bottom-link">
          <Link className="btn secondary" to={`/rooms/${room.id}`}>
            Назад до кімнати
          </Link>
        </div>
      </main>

      <PageFooter/>
    </div>
  )
}
