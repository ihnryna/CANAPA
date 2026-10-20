import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
    getRoomRecommendations,
    type RoomRecommendedMovie,
} from '../api/recommendations'
import { useCurrentUser } from '../auth/current-user-context'
import { isUuid } from '../auth/user-id'
import { AppHeader } from '../components/AppHeader'
import { Icon } from '../components/Icon'
import { PageFooter } from '../components/PageFooter'

interface ResultsProps {
    roomId: string
    userId: string
}

function RecommendationResults({ roomId, userId }: ResultsProps) {
    const [movies, setMovies] = useState<RoomRecommendedMovie[] | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [attempt, setAttempt] = useState(0)

    useEffect(() => {
        const controller = new AbortController()

        getRoomRecommendations(
            roomId,
            userId,
            'CONSENSUS',
            40,
            controller.signal,
        )
            .then((result) => {
                if (!controller.signal.aborted) {
                    setMovies(result)
                }
            })
            .catch((caught: unknown) => {
                if (!controller.signal.aborted) {
                    setError(
                        caught instanceof Error
                            ? caught.message
                            : 'Не вдалося отримати рекомендації.',
                    )
                }
            })

        return () => controller.abort()
    }, [roomId, userId, attempt])

    function retry() {
        setMovies(null)
        setError(null)
        setAttempt((current) => current + 1)
    }

    if (error !== null) {
        return (
            <div className="inline-alert" role="alert">
                <h2>Не вдалося завантажити рекомендації</h2>
                <p>{error}</p>
                <button className="btn secondary" onClick={retry}>
                    Спробувати ще раз
                </button>
            </div>
        )
    }

    if (movies === null) {
        return (
            <section className="panel" aria-busy="true">
                <p>Завантажуємо рекомендації...</p>
            </section>
        )
    }

    if (movies.length === 0) {
        return (
            <section className="panel">
                <h2>Рекомендацій поки немає</h2>
                <p>
                    Перевірте, чи додані фільми та вподобання
                    учасників кімнати.
                </p>
            </section>
        )
    }

    return (
        <section className="panel">
            <h2>Рекомендовані фільми ({movies.length})</h2>

            <ul>
                {movies.map(({ movie, score }) => (
                    <li key={movie.id}>
                        <strong>{movie.title}</strong>
                        {' — '}
                        {Math.round(score * 100)}% відповідності
                        {movie.releaseYear !== null && (
                            <span> ({movie.releaseYear})</span>
                        )}
                    </li>
                ))}
            </ul>
        </section>
    )
}

export function RecommendationPage() {
    const { roomId } = useParams()
    const { user } = useCurrentUser()

    const validRoomId =
        roomId !== undefined && isUuid(roomId)
            ? roomId
            : null

    return (
        <div className="app-shell">
            <AppHeader />

            <main className="main-content">
                <Link
                    className="back-link"
                    to={validRoomId ? `/rooms/${validRoomId}` : '/rooms'}
                >
                    <Icon name="back" size={16} />
                    Назад до кімнати
                </Link>

                <div className="page-heading">
                    <div className="eyebrow">CANAPA / РЕКОМЕНДАЦІЇ</div>
                    <h1>Фільми для вашої компанії</h1>
                    <p>Добірка за спільними вподобаннями учасників.</p>
                </div>

                {validRoomId === null ? (
                    <p className="form-error" role="alert">
                        Некоректний ID кімнати.
                    </p>
                ) : user !== null ? (
                    <RecommendationResults
                        key={`${validRoomId}:${user.id}`}
                        roomId={validRoomId}
                        userId={user.id}
                    />
                ) : null}
            </main>

            <PageFooter />
        </div>
    )
}
