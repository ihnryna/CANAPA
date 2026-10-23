import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import type { Movie } from '../api/movies'
import { getRoomRecommendations } from '../api/recommendations'
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
import { RoomPageHeader } from "../components/RoomPageHeader.tsx";

const USE_MOCK_VOTING = false
const VOTING_MOVIE_LIMIT = 15

const STATUS_LABELS = {
  CREATED: 'Підготовка',
  VOTING: 'Голосування триває',
  FINISHED: 'Завершено',
} as const

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
    lastUpdatedAt,
    refresh: refreshRoom,
  } = useRoomPolling(validRoomId)
  const navigate = useNavigate()
  const member = room?.participants.some((participant) => participant.id === user?.id) ?? false

  const [movies, setMovies] = useState<Movie[]>([])
  const [votes, setVotes] = useState<Vote[]>([])
  const [excludedMovies, setExcludedMovies] = useState<ExcludedMovie[]>([])
  const [result, setResult] = useState<VotingResult | null>(null)

  const [loadingMovies, setLoadingMovies] = useState(true)
  const [pendingMovieId, setPendingMovieId] = useState<number | null>(null)
  const [finishing, setFinishing] = useState(false)
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

  const participantsWithoutVotes = useMemo(() => {
    if (room === null) return []

    const voterIds = new Set(votes.map((vote) => vote.userId))
    const requiredParticipants = new Map(
      room.participants.map((participant) => [participant.id, participant]),
    )
    requiredParticipants.set(room.creator.id, room.creator)

    return [...requiredParticipants.values()].filter(
      (participant) => !voterIds.has(participant.id),
    )
  }, [room, votes])

  const allParticipantsVoted = participantsWithoutVotes.length === 0

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

    if (
      validRoomId === undefined ||
      user === null ||
      !isUuid(user.id) ||
      room === null
    ) {
      setLoadingMovies(false)
      return
    }

    setError(null)
    setLoadingMovies(true)

    try {
      const [loadedMovies, loadedVotes, loadedExcluded] =
        await Promise.all([
          getRoomRecommendations(
            validRoomId,
            user.id,
            'CONSENSUS',
            VOTING_MOVIE_LIMIT,
          ).then((recommendations) =>
            recommendations.map((recommendation) => recommendation.movie),
          ),
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
  }, [validRoomId, user, room?.status])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void loadData()
    }, 0)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [loadData])

  if (room === null)
    return

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
    if (
      validRoomId === undefined ||
      !organizer ||
      !isVoting ||
      !allParticipantsVoted
    ) {
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
        winners: MOCK_MOVIES.filter((movie) =>
          winnerMovieIds.includes(movie.id)
        ),
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
      await finishVoting(validRoomId)
      navigate(`/rooms/${validRoomId}/results`, { replace: true })
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
        <RoomPageHeader
          error={roomError}
          refresh={refreshRoom}
          member={member}
          room={room}
          STATUS_LABELS={STATUS_LABELS}
          lastUpdatedAt={lastUpdatedAt}
          navigate={navigate}
          currentIdx={2}
        />

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
                    const movie = result.winners.find(
                      (winner) => winner.id === id
                    ) ?? movieById.get(id)

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
              Перевірте вподобання учасників і оновіть список рекомендацій.
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
              {allParticipantsVoted ? (
                <p>
                  Усі учасники проголосували. Завершення зафіксує результат.
                </p>
              ) : (
                <p>
                  Ще не проголосували: {' '}
                  {participantsWithoutVotes
                    .map((participant) => participant.name)
                    .join(', ')}.
                </p>
              )}
            </div>
            <button
              className="btn primary"
              disabled={finishing || !allParticipantsVoted}
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
