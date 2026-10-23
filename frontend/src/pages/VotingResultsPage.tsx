import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'

import {
  getVotingResult,
  type VotingResult,
} from '../api/voting'
import { useCurrentUser } from '../auth/current-user-context'
import { isUuid } from '../auth/user-id'
import { AppHeader } from '../components/AppHeader'
import { Icon } from '../components/Icon'
import { PageFooter } from '../components/PageFooter'
import { RoomPageHeader } from '../components/RoomPageHeader'
import { useRoomPolling } from '../hooks/useRoomPolling'

const STATUS_LABELS = {
  CREATED: 'Підготовка',
  VOTING: 'Голосування триває',
  FINISHED: 'Завершено',
} as const

export function VotingResultsPage() {
  const { roomId } = useParams()
  const validRoomId =
    roomId !== undefined && isUuid(roomId) ? roomId : undefined

  const { user } = useCurrentUser()
  const navigate = useNavigate()

  const {
    room,
    loading: roomLoading,
    error: roomError,
    lastUpdatedAt,
    refresh: refreshRoom,
  } = useRoomPolling(validRoomId)

  const [result, setResult] = useState<VotingResult | null>(null)
  const [loadingResult, setLoadingResult] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const member =
    room?.participants.some(
      (participant) => participant.id === user?.id,
    ) ?? false


  const loadResult = useCallback(async () => {
    if (!validRoomId || !user || !isUuid(user.id)) {
      setLoadingResult(false)
      return
    }

    if (room?.status !== 'FINISHED') {
      setResult(null)
      setLoadingResult(false)
      return
    }

    setLoadingResult(true)
    setError(null)

    try {
      const votingResult = await getVotingResult(validRoomId)
      setResult(votingResult)
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : 'Не вдалося завантажити результати голосування.',
      )
    } finally {
      setLoadingResult(false)
    }
  }, [validRoomId, user, room?.status])

  useEffect(() => {
    void loadResult()
  }, [loadResult])

  if (validRoomId === undefined) {
    return (
      <div className="app-shell">
        <AppHeader />
        <main className="centered-state">
          <h1>Некоректний ID кімнати</h1>
          <Link className="btn primary" to="/rooms">
            До моїх кімнат
          </Link>
        </main>
        <PageFooter />
      </div>
    )
  }

  if (roomLoading && room === null) {
    return (
      <div className="app-shell">
        <AppHeader />
        <main className="centered-state" aria-busy="true">
          <span className="spinner" />
          <h1>Завантажуємо кімнату…</h1>
        </main>
        <PageFooter />
      </div>
    )
  }

  if (room === null) {
    return (
      <div className="app-shell">
        <AppHeader />
        <main className="centered-state">
          <h1>Не вдалося відкрити кімнату</h1>
          <p role="alert">
            {roomError ?? 'Кімнату не знайдено.'}
          </p>
          <Link className="btn primary" to="/rooms">
            До моїх кімнат
          </Link>
        </main>
        <PageFooter />
      </div>
    )
  }

  if (user === null || !isUuid(user.id)) {
    return (
      <div className="app-shell">
        <AppHeader />
        <main className="centered-state">
          <h1>Спочатку оберіть профіль</h1>
          <p>Для перегляду результатів потрібен профіль CANAPA.</p>
          <Link className="btn primary" to="/">
            Обрати профіль
          </Link>
        </main>
        <PageFooter />
      </div>
    )
  }

  return (
    <div className="app-shell">
      <AppHeader />

      <main className="main-content voting-page">
        <RoomPageHeader
          error={roomError}
          refresh={refreshRoom}
          member={member}
          room={room}
          STATUS_LABELS={STATUS_LABELS}
          lastUpdatedAt={lastUpdatedAt}
          navigate={navigate}
          currentIdx={3}
        />

        {error !== null && (
          <div className="voting-alert" role="alert">
            <span>{error}</span>
            <button
              className="text-button"
              onClick={() => void loadResult()}
            >
              Спробувати ще раз
            </button>
          </div>
        )}

        {room.status !== 'FINISHED' ? (
          <section className="voting-state-panel">
            <h2>Результатів ще немає</h2>
            <p>
              Результати стануть доступними після завершення голосування.
            </p>
            <Link
              className="btn primary"
              to={`/rooms/${room.id}/voting`}
>
До голосування
</Link>
</section>
) : loadingResult ? (
    <div className="centered-state compact-state" aria-busy="true">
        <span className="spinner" />
        <p>Завантажуємо результати…</p>
    </div>
) : result === null ? (
    <section className="voting-state-panel">
        <h2>Не вдалося завантажити результати</h2>
        <button
            className="btn primary"
            onClick={() => void loadResult()}
        >
            Спробувати ще раз
        </button>
    </section>
) : (
    <section className="voting-state-panel result-panel">
        <div className="eyebrow">РЕЗУЛЬТАТ</div>
        <h1>Голосування завершено</h1>

        {result.winnerMovieIds.length === 0 ? (
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
                            (winner) => winner.id === id,
                        )

                        return (
                            <div className="winner-item" key={id}>
                                {movie?.posterUrl ? (
                                    <img src={movie.posterUrl} alt="" />
                                ) : (
                                    <span className="poster-placeholder">
                            <Icon name="film" size={28} />
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
                    Загальна кількість голосів за переможців:{' '}
                    {result.voteCount}
                </p>
            </>
        )}
    </section>
)}

<div className="voting-bottom-link">
    <Link
        className="btn secondary"
        to={`/rooms/${room.id}/voting`}
    >
        До голосування
    </Link>

    <Link className="btn secondary" to={`/rooms/${room.id}`}>
        Назад до кімнати
    </Link>
</div>
</main>

<PageFooter />
</div>
)
}
