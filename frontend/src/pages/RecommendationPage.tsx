import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
    getRoomRecommendations,
    type RecommendationType,
    type RoomRecommendedMovie,
} from '../api/recommendations'
import { useCurrentUser } from '../auth/current-user-context'
import { isUuid } from '../auth/user-id'
import { AppHeader } from '../components/AppHeader'
import { Icon } from '../components/Icon'
import { PageFooter } from '../components/PageFooter'
import './RecommendationPage.css'

const strategies: {
    value: RecommendationType
    label: string
}[] = [
    { value: 'CONSENSUS', label: 'Спільний вибір' },
    { value: 'AVERAGE_RATING', label: 'За рейтингом' },
    { value: 'GENRE_FOCUSED', label: 'За жанрами' },
]

type SortOrder = 'match' | 'rating'

type LoadResult = {
    key: string
    movies: RoomRecommendedMovie[]
    error: string | null
}

function RecommendationCard({
                                item,
                            }: {
    item: RoomRecommendedMovie
}) {
    const { movie, score } = item
    const [posterFailed, setPosterFailed] = useState(false)

    return (
        <article className="recommendation-card">
            <div className="recommendation-poster">
                {movie.posterUrl && !posterFailed ? (
                    <img
                        src={movie.posterUrl}
                        alt={`Постер фільму «${movie.title}»`}
                        loading="lazy"
                        onError={() => setPosterFailed(true)}
                    />
                ) : (
                    <div className="recommendation-poster-fallback">
                        <Icon name="film" size={42} />
                        <span>Постер відсутній</span>
                    </div>
                )}

                <span className="recommendation-match">
          {Math.round(
              Math.max(0, Math.min(1, score)) * 100,
          )}% · оцінка
        </span>
            </div>

            <div className="recommendation-card-body">
                <p className="recommendation-meta">
                    {movie.releaseYear ?? 'Рік невідомий'}
                    {movie.runtime != null
                        ? ` · ${movie.runtime} хв`
                        : ''}
                    {movie.rating != null
                        ? ` · ★ ${movie.rating.toFixed(1)}`
                        : ''}
                </p>

                <h2>{movie.title}</h2>

                <div className="recommendation-genres">
                    {movie.genres.map((genre) => (
                        <span key={genre}>{genre}</span>
                    ))}
                </div>

                {movie.description && (
                    <p className="recommendation-description">
                        {movie.description}
                    </p>
                )}
            </div>
        </article>
    )
}

function RecommendationResults({
                                   roomId,
                                   userId,
                               }: {
    roomId: string
    userId: string
}) {
    const [strategy, setStrategy] =
        useState<RecommendationType>('CONSENSUS')

    const [sortOrder, setSortOrder] =
        useState<SortOrder>('match')

    const [query, setQuery] = useState('')
    const [retryCount, setRetryCount] = useState(0)
    const [result, setResult] = useState<LoadResult | null>(null)

    const requestKey = `${strategy}:${retryCount}`

    useEffect(() => {
        const controller = new AbortController()

        getRoomRecommendations(
            roomId,
            userId,
            strategy,
            40,
            controller.signal,
        )
            .then((movies) => {
                if (!controller.signal.aborted) {
                    setResult({
                        key: requestKey,
                        movies,
                        error: null,
                    })
                }
            })
            .catch((error: unknown) => {
                if (!controller.signal.aborted) {
                    setResult({
                        key: requestKey,
                        movies: [],
                        error:
                            error instanceof Error
                                ? error.message
                                : 'Помилка завантаження.',
                    })
                }
            })

        return () => controller.abort()
    }, [roomId, userId, strategy, requestKey])

    const current = result?.key === requestKey
        ? result
        : null

    const search = query.trim().toLocaleLowerCase('uk')

    const visibleMovies = (current?.movies ?? [])
        .filter(({ movie }) =>
            movie.title.toLocaleLowerCase('uk').includes(search) ||
            movie.genres.some((genre) =>
                genre.toLocaleLowerCase('uk').includes(search),
            ),
        )
        .sort((a, b) =>
            sortOrder === 'rating'
                ? (b.movie.rating ?? -1) -
                (a.movie.rating ?? -1)
                : b.score - a.score,
        )

    return (
        <section
            aria-label="Рекомендовані фільми"
            className="recommendations-section"
        >
            <div className="recommendations-toolbar">
                <div
                    className="recommendations-tabs"
                    role="group"
                    aria-label="Сортування фільмів"
                >
                    <button
                        type="button"
                        className={sortOrder === 'match' ? 'active' : ''}
                        aria-pressed={sortOrder === 'match'}
                        onClick={() => setSortOrder('match')}
                    >
                        Рекомендовані
                    </button>

                    <button
                        type="button"
                        className={sortOrder === 'rating' ? 'active' : ''}
                        aria-pressed={sortOrder === 'rating'}
                        onClick={() => setSortOrder('rating')}
                    >
                        За рейтингом
                    </button>
                </div>

                <label className="recommendations-strategy">
                    Алгоритм
                    <select
                        value={strategy}
                        onChange={(event) =>
                            setStrategy(
                                event.target.value as RecommendationType,
                            )
                        }
                    >
                        {strategies.map(({ value, label }) => (
                            <option key={value} value={value}>
                                {label}
                            </option>
                        ))}
                    </select>
                </label>

                <label className="recommendations-search">
          <span className="recommendations-sr-only">
            Пошук за назвою або жанром
          </span>
                    <input
                        type="search"
                        placeholder="Знайти фільм або жанр..."
                        value={query}
                        onChange={(event) =>
                            setQuery(event.target.value)
                        }
                    />
                </label>
            </div>

            {current === null ? (
                <div className="recommendations-state" role="status">
                    Завантажуємо рекомендації...
                </div>
            ) : current.error !== null ? (
                <div className="recommendations-state" role="alert">
                    <h2>Не вдалося завантажити рекомендації</h2>
                    <p>{current.error}</p>
                    <button
                        type="button"
                        className="btn secondary"
                        onClick={() =>
                            setRetryCount((n) => n + 1)
                        }
                    >
                        Спробувати ще раз
                    </button>
                </div>
            ) : current.movies.length === 0 ? (
                <div className="recommendations-state">
                    <h2>Рекомендацій поки немає</h2>
                    <p>
                        Можливо, у каталозі ще немає фільмів
                        або для обраного алгоритму бракує вподобань.
                    </p>
                </div>
            ) : visibleMovies.length === 0 ? (
                <div className="recommendations-state">
                    <h2>Нічого не знайдено</h2>
                    <p>Спробуй іншу назву або жанр.</p>
                </div>
            ) : (
                <>
                    <p className="recommendations-count">
                        Знайдено фільмів: {visibleMovies.length}
                    </p>

                    <div className="recommendations-grid">
                        {visibleMovies.map((item) => (
                            <RecommendationCard
                                key={item.movie.id}
                                item={item}
                            />
                        ))}
                    </div>
                </>
            )}
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
                    to={
                        validRoomId
                            ? `/rooms/${validRoomId}`
                            : '/rooms'
                    }
                >
                    <Icon name="back" size={16} />
                    Назад до кімнати
                </Link>

                <div className="page-heading">
                    <div className="eyebrow">
                        CANAPA / РЕКОМЕНДАЦІЇ
                    </div>
                    <h1>Фільми для вашої компанії</h1>
                    <p>
                        Обери алгоритм та переглянь добірку
                        за вподобаннями учасників.
                    </p>
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
