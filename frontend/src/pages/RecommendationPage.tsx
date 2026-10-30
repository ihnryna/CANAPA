import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getPopularMovies, searchMovies, type Movie } from '../api/movies'
import {
  getRoomRecommendations,
  type RecommendationType,
  type RoomRecommendedMovie,
} from '../api/recommendations'
import { useCurrentUser } from '../auth/current-user-context'
import { isUuid } from '../auth/user-id'
import { Icon } from '../components/Icon'
import { PageFooter } from '../components/PageFooter'
import { useRoomPolling } from '../hooks/useRoomPolling'
import './RecommendationPage.css'

const strategies: { value: RecommendationType; label: string }[] = [
  { value: 'CONSENSUS', label: 'Спільний вибір' },
  { value: 'AVERAGE_RATING', label: 'За рейтингом' },
  { value: 'GENRE_FOCUSED', label: 'За жанрами' },
]

const tabs = ['Рекомендовані', 'Популярні', 'Пошук'] as const
type MovieTab = typeof tabs[number]
type SortOrder = 'match' | 'rating'
type MovieItem = { movie: Movie; score: number | null }
type LoadResult = {
  key: string
  movies: RoomRecommendedMovie[]
  error: string | null
}
type CatalogResult = {
  key: string
  movies: Movie[]
  totalPages: number
  totalResults: number
  error: string | null
}

const statusLabels = {
  CREATED: 'Підготовка',
  VOTING: 'Голосування триває',
  FINISHED: 'Завершено',
} as const

// There is no room-candidate API. Do not turn votes or local state into a shortlist.
const selectionUnavailable =
  'Додавання фільмів до спільної добірки ще недоступне.'

function RecommendationIcon({ name, size = 17 }: { name: 'search' | 'star'; size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none"
         stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"
         strokeLinejoin="round" aria-hidden="true">
      {name === 'search'
        ? <>
          <circle cx="10" cy="10" r="6"/>
          <path d="m15 15 5 5"/>
        </>
        : <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9Z"/>}
    </svg>
  )
}

function formatRating(rating: number | null) {
  return rating !== null && Number.isFinite(rating)
    ? rating.toFixed(1).replace('.', ',')
    : '—'
}

function RecommendationScore({ score }: { score: number | null }) {
  const available = score !== null && Number.isFinite(score)
  return (
    <span className="recommendations-match">
      {available && <Icon name="check" size={13}/>}
      {available
        ? `Рекомендаційна оцінка: ${Math.round(Math.max(0, Math.min(1, score)) * 100)}%`
        : 'Рекомендаційна оцінка недоступна'}
    </span>
  )
}

function MoviePoster({ movie }: { movie: Movie }) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  return movie.posterUrl && movie.posterUrl !== failedUrl ? (
    <img src={movie.posterUrl} alt={`Постер фільму «${movie.title}»`}
         loading="lazy" onError={() => setFailedUrl(movie.posterUrl)}/>
  ) : (
    <span className="recommendations-poster-fallback">
      <Icon name="film" size={42}/>
      <span>Постер відсутній</span>
    </span>
  )
}

function RecommendationCard({ item, onDetails }: {
  item: MovieItem
  onDetails: (item: MovieItem) => void
}) {
  const { movie, score } = item
  return (
    <article className="recommendations-card">
      <button type="button" className="recommendations-poster"
              aria-label={`Детальніше про «${movie.title}»`} onClick={() => onDetails(item)}>
        <MoviePoster movie={movie}/>
        <span className="recommendations-rating" aria-label={`Рейтинг: ${formatRating(movie.rating)}`}>
          <RecommendationIcon name="star" size={13}/>
          {formatRating(movie.rating)}
        </span>
        <span className="recommendations-poster-details">
          Детальніше <Icon name="arrow" size={17}/>
        </span>
      </button>
      <div className="recommendations-card-body">
        <small>
          {movie.releaseYear ?? 'Рік невідомий'}
          {movie.runtime !== null && ` · ${movie.runtime} хв`}
        </small>
        <button type="button" className="recommendations-movie-title" onClick={() => onDetails(item)}>
          {movie.title}
        </button>
        <p>{movie.genres.length > 0 ? movie.genres.join(', ') : 'Жанри не вказано'}</p>
        <RecommendationScore score={score}/>
        <button type="button" className="recommendations-button primary full" disabled
                title={selectionUnavailable} aria-describedby="recommendations-selection-note">
          <Icon name="plus" size={17}/> Додати
        </button>
      </div>
    </article>
  )
}

function MovieDetails({ item, onClose }: { item: MovieItem; onClose: () => void }) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const closeRef = useRef<HTMLButtonElement>(null)
  const { movie, score } = item

  useEffect(() => {
    const dialog = dialogRef.current
    const trigger = document.activeElement
    const previousOverflow = document.body.style.overflow
    dialog?.showModal()
    closeRef.current?.focus()
    document.body.style.overflow = 'hidden'
    return () => {
      dialog?.close()
      document.body.style.overflow = previousOverflow
      if (trigger instanceof HTMLElement && trigger.isConnected) trigger.focus()
    }
  }, [onClose])

  return (
    <dialog ref={dialogRef} className="recommendations-dialog"
            aria-labelledby="recommendations-detail-title"
            onCancel={(event) => {
              event.preventDefault();
              onClose()
            }}
            onClick={(event) => {
              if (event.target !== event.currentTarget) return
              const bounds = event.currentTarget.getBoundingClientRect()
              if (event.clientX < bounds.left || event.clientX > bounds.right
                || event.clientY < bounds.top || event.clientY > bounds.bottom) onClose()
            }}>
      <button ref={closeRef} type="button" className="recommendations-modal-close"
              aria-label="Закрити" onClick={onClose}><Icon name="close"/></button>
      <div className="recommendations-detail-poster"><MoviePoster movie={movie}/></div>
      <div className="recommendations-detail-content">
        <h2 id="recommendations-detail-title">{movie.title}</h2>
        <p>{[movie.releaseYear, movie.genres.join(', ')].filter(Boolean).join(' · ')}</p>
        <div className="recommendations-chips">
          <span>Рейтинг: {formatRating(movie.rating)}</span>
          {movie.runtime !== null && <span>Тривалість: {movie.runtime} хв</span>}
          {movie.rated && <span>{movie.rated}</span>}
        </div>
        <p>{movie.description || 'Опис фільму поки відсутній.'}</p>
        {(movie.originalLanguage || movie.director || movie.writers) && (
          <p>
            {movie.originalLanguage &&
                <span className="recommendations-detail-line">Мова: {movie.originalLanguage}</span>}
            {movie.director && <span className="recommendations-detail-line">Режисер: {movie.director}</span>}
            {movie.writers && <span className="recommendations-detail-line">Сценарист: {movie.writers}</span>}
          </p>
        )}
        {movie.actors && <p>У ролях: {movie.actors}</p>}
        <RecommendationScore score={score}/>
        <button type="button" className="recommendations-button primary" disabled
                aria-describedby="recommendations-detail-note">
          Додати до добірки <Icon name="arrow" size={17}/>
        </button>
        <p id="recommendations-detail-note" className="recommendations-unavailable">{selectionUnavailable}</p>
      </div>
    </dialog>
  )
}

function RecommendationResults({ roomId, userId }: { roomId: string; userId: string }) {
  const { room, error: roomError, refresh } = useRoomPolling(roomId)
  const [strategy, setStrategy] = useState<RecommendationType>('CONSENSUS')
  const [sortOrder, setSortOrder] = useState<SortOrder>('match')
  const [tab, setTab] = useState<MovieTab>('Рекомендовані')
  const [query, setQuery] = useState('')
  const [searchPage, setSearchPage] = useState(1)
  const [retryCount, setRetryCount] = useState(0)
  const [catalogRetry, setCatalogRetry] = useState(0)
  const [result, setResult] = useState<LoadResult | null>(null)
  const [catalog, setCatalog] = useState<CatalogResult | null>(null)
  const [detail, setDetail] = useState<MovieItem | null>(null)
  const searchRef = useRef<HTMLInputElement>(null)
  const closeDetails = useCallback(() => setDetail(null), [])
  const requestKey = `${strategy}:${retryCount}`
  const catalogQuery = tab === 'Пошук' ? query.trim() : ''
  const catalogKey = JSON.stringify([tab, catalogQuery, searchPage, catalogRetry])

  useEffect(() => {
    const controller = new AbortController()
    getRoomRecommendations(roomId, userId, strategy, 15, controller.signal)
      .then((movies) => {
        if (!controller.signal.aborted) setResult({ key: requestKey, movies, error: null })
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setResult({
            key: requestKey,
            movies: [],
            error: error instanceof Error ? error.message : 'Помилка завантаження.',
          })
        }
      })
    return () => controller.abort()
  }, [roomId, userId, strategy, requestKey])

  useEffect(() => {
    if (tab === 'Рекомендовані' || (tab === 'Пошук' && !catalogQuery)) return
    // The existing catalog helpers do not accept AbortSignal. Ignore late responses.
    let active = true
    const timer = window.setTimeout(async () => {
      try {
        const loaded = tab === 'Популярні'
          ? await getPopularMovies(40).then((movies) => ({
            results: movies, totalPages: 1, totalResults: movies.length,
          }))
          : await searchMovies(catalogQuery, searchPage, 40)
        if (active) {
          setCatalog({
            key: catalogKey,
            movies: loaded.results,
            totalPages: loaded.totalPages,
            totalResults: loaded.totalResults,
            error: null,
          })
        }
      } catch (error: unknown) {
        if (active) {
          setCatalog({
            key: catalogKey, movies: [], totalPages: 0, totalResults: 0,
            error: error instanceof Error ? error.message : 'Помилка завантаження фільмів.',
          })
        }
      }
    }, tab === 'Пошук' ? 350 : 0)
    return () => {
      active = false;
      window.clearTimeout(timer)
    }
  }, [tab, catalogQuery, searchPage, catalogKey])

  const currentRecommendations = result?.key === requestKey ? result : null
  const currentCatalog = catalog?.key === catalogKey ? catalog : null
  const current = tab === 'Рекомендовані' ? currentRecommendations : currentCatalog
  const search = query.trim().toLocaleLowerCase('uk')
  const items: MovieItem[] = tab === 'Рекомендовані'
    ? currentRecommendations?.movies ?? []
    : (currentCatalog?.movies ?? []).map((movie) => ({ movie, score: null }))
  const visibleMovies = items
    .filter(({ movie }) => tab === 'Пошук'
      || movie.title.toLocaleLowerCase('uk').includes(search)
      || movie.genres.some((genre) => genre.toLocaleLowerCase('uk').includes(search)))
    .sort((a, b) => sortOrder === 'rating'
      ? (b.movie.rating ?? -1) - (a.movie.rating ?? -1)
      : (b.score ?? -1) - (a.score ?? -1))
  const searchPrompt = tab === 'Пошук' && !catalogQuery
  const canOpenVoting = room?.status === 'VOTING' && !roomError
    && room.participants.some((participant) => participant.id === userId)

  function changeTab(nextTab: MovieTab) {
    setTab(nextTab)
    setSearchPage(1)
    setSortOrder(nextTab === 'Популярні' ? 'rating' : 'match')
    if (nextTab === 'Пошук') searchRef.current?.focus()
  }

  return (
    <>
      <Link className="recommendations-back" to="/rooms"><Icon name="back" size={16}/> Мої кімнати</Link>
      <div className="recommendations-room-top">
        <div>
          <div className="recommendations-eyebrow">ВАША КІНОКОМПАНІЯ</div>
          <h2>{room?.name ?? (roomError ? 'Кімната недоступна' : 'Завантажуємо кімнату…')}</h2>
        </div>
        {room && !roomError && (
          <span className="recommendations-status"><span/>{statusLabels[room.status]}</span>
        )}
      </div>
      {roomError && (
        <div className="recommendations-room-error" role="alert">
          <p>{roomError}</p>
          <button type="button" onClick={() => void refresh()}>Спробувати ще раз</button>
        </div>
      )}
      <nav className="recommendations-steps" aria-label="Етапи кіновечора">
        <Link to={`/rooms/${roomId}`}><span>01</span>Учасники</Link>
        <button type="button" disabled title="Сторінка вподобань ще недоступна"><span>02</span>Уподобання</button>
        <Link to={`/rooms/${roomId}/recommendations`} className="active"
              aria-current="step"><span>03</span>Добірка</Link>
        {canOpenVoting
          ? <Link to={`/rooms/${roomId}/voting`}><span>04</span>Голосування</Link>
          : <button type="button" disabled title="Голосування ще не відкрите"><span>04</span>Голосування</button>}
        <button type="button" disabled title="Окрема сторінка результату ще недоступна"><span>05</span>Результат
        </button>
      </nav>

      <div className="recommendations-heading">
        <div className="recommendations-eyebrow">КРОК 03 · СПІЛЬНА ДОБІРКА</div>
        <h1>Оберіть кандидатів</h1>
        <p>Додайте від 5 до 15 фільмів, між якими голосуватиме група.</p>
      </div>

      <section aria-label="Фільми для спільної добірки">
        <div className="recommendations-toolbar">
          <div className="recommendations-tabs" role="group" aria-label="Джерело фільмів">
            {tabs.map((name) => (
              <button key={name} type="button" className={tab === name ? 'active' : ''}
                      aria-pressed={tab === name} onClick={() => changeTab(name)}>{name}</button>
            ))}
            <button type="button" disabled title={selectionUnavailable}
                    aria-describedby="recommendations-selection-note">У добірці
            </button>
          </div>
          <label className="recommendations-search">
            <RecommendationIcon name="search"/>
            <span className="recommendations-sr-only">
              {tab === 'Пошук' ? 'Знайти фільм у каталозі' : 'Пошук за назвою або жанром'}
            </span>
            <input ref={searchRef} type="search" placeholder="Введіть назву фільму" value={query}
                   onChange={(event) => {
                     setQuery(event.target.value);
                     setSearchPage(1)
                   }}/>
          </label>
        </div>
        <div className="recommendations-controls">
          <p className="recommendations-count" role="status">
            {searchPrompt ? 'Пошук у каталозі'
              : current === null ? 'Завантажуємо фільми…'
                : current.error ? 'Не вдалося завантажити фільми'
                  : `Знайдено фільмів: ${tab === 'Пошук' ? currentCatalog?.totalResults ?? 0 : visibleMovies.length}`}
          </p>
          {tab === 'Рекомендовані' && (
            <label className="recommendations-select">
              Алгоритм
              <select value={strategy} onChange={(event) => setStrategy(event.target.value as RecommendationType)}>
                {strategies.map(({ value, label }) => <option key={value} value={value}>{label}</option>)}
              </select>
            </label>
          )}
          <label className="recommendations-select">
            Сортування
            <select value={sortOrder} onChange={(event) => setSortOrder(event.target.value as SortOrder)}>
              <option
                value="match">{tab === 'Рекомендовані' ? 'За рекомендаційною оцінкою' : 'За порядком результатів'}</option>
              <option value="rating">{tab === 'Пошук' ? 'За рейтингом на сторінці' : 'За рейтингом'}</option>
            </select>
          </label>
        </div>

        {searchPrompt ? (
          <div className="recommendations-state">
            <RecommendationIcon name="search" size={32}/>
            <h2>Знайдіть фільм</h2>
            <p>Введіть назву фільму в пошуковому полі.</p>
          </div>
        ) : current === null ? (
          <div className="recommendations-state" role="status" aria-busy="true">
            <span className="recommendations-spinner"/>
            {tab === 'Рекомендовані' ? 'Завантажуємо рекомендації…' : 'Завантажуємо фільми…'}
          </div>
        ) : current.error !== null ? (
          <div className="recommendations-state" role="alert">
            <h2>{tab === 'Рекомендовані' ? 'Не вдалося завантажити рекомендації' : 'Не вдалося завантажити фільми'}</h2>
            <p>{current.error}</p>
            <button type="button" className="recommendations-button secondary" onClick={() => {
              if (tab === 'Рекомендовані') setRetryCount((count) => count + 1)
              else setCatalogRetry((count) => count + 1)
            }}>Спробувати ще раз
            </button>
          </div>
        ) : items.length === 0 && tab === 'Рекомендовані' ? (
          <div className="recommendations-state">
            <h2>Рекомендацій поки немає</h2>
            <p>Можливо, у каталозі ще немає фільмів або для обраного алгоритму бракує вподобань.</p>
          </div>
        ) : visibleMovies.length === 0 ? (
          <div className="recommendations-state">
            <h2>{tab === 'Популярні' && !search ? 'У каталозі поки немає фільмів' : 'Нічого не знайдено'}</h2>
            <p>{tab === 'Пошук' ? 'Спробуйте іншу назву фільму.' : 'Спробуйте іншу назву або жанр, чи відкрийте вкладку «Пошук».'}</p>
          </div>
        ) : (
          <div className="recommendations-grid">
            {visibleMovies.map((item) => <RecommendationCard key={item.movie.id} item={item} onDetails={setDetail}/>)}
          </div>
        )}
        {tab === 'Пошук' && !searchPrompt && currentCatalog && !currentCatalog.error && currentCatalog.totalPages > 1 && (
          <nav className="recommendations-pagination" aria-label="Сторінки пошуку">
            <button type="button" className="recommendations-button secondary"
                    disabled={searchPage <= 1} onClick={() => setSearchPage((page) => page - 1)}>Назад
            </button>
            <span>Сторінка {searchPage} з {currentCatalog.totalPages}</span>
            <button type="button" className="recommendations-button secondary"
                    disabled={searchPage >= currentCatalog.totalPages}
                    onClick={() => setSearchPage((page) => page + 1)}>Далі <Icon name="arrow" size={17}/></button>
          </nav>
        )}
        <div className="recommendations-bottom-bar">
          <div>
            <strong>Добірка поки недоступна</strong>
            <small>Рекомендована кількість: від 5 до 15</small>
          </div>
          {canOpenVoting ? (
            <Link className="recommendations-button primary" to={`/rooms/${roomId}/voting`}>
              Продовжити до голосування <Icon name="arrow" size={17}/>
            </Link>
          ) : (
            <button type="button" className="recommendations-button primary" disabled
                    aria-describedby="recommendations-selection-note">
              Продовжити до голосування <Icon name="arrow" size={17}/>
            </button>
          )}
        </div>
        <p id="recommendations-selection-note" className="recommendations-unavailable">
          {selectionUnavailable} {canOpenVoting
          ? 'Голосування кімнати вже відкрите; фільми з цього екрана до нього не додаються.'
          : room?.status === 'FINISHED' ? 'Голосування кімнати завершено.'
            : 'Перехід до голосування стане доступним, коли його буде відкрито в кімнаті.'}
        </p>
      </section>
      {detail && <MovieDetails item={detail} onClose={closeDetails}/>}
    </>
  )
}

export function RecommendationPage() {
  const { roomId } = useParams()
  const { user } = useCurrentUser()
  const validRoomId = roomId !== undefined && isUuid(roomId) ? roomId : null

  return (
    <div className="recommendations-page" lang="uk">
      <header className="recommendations-header">
        <div className="recommendations-header-inner">
          <Link className="recommendations-brand" to="/rooms" aria-label="CANAPA — на головну">
            <span><Icon name="sofa" size={25}/></span>CANAPA<span className="recommendations-brand-dot">.</span>
          </Link>
          <nav aria-label="Основна навігація">
            <Link to="/rooms">Мої кімнати</Link>
            <Link to="/setup">Мій профіль</Link>
          </nav>
          <div className="recommendations-header-right">
            <span>Кіно об’єднує</span>
            {user && (
              <Link className="recommendations-avatar" to="/setup" aria-label={`Профіль ${user.name}`}>
                {user.name.trim().charAt(0).toUpperCase()}
              </Link>
            )}
          </div>
        </div>
      </header>
      <main className="recommendations-main">
        {validRoomId === null ? (
          <div className="recommendations-state" role="alert">
            <h1>Некоректний ID кімнати</h1>
            <Link className="recommendations-button secondary" to="/rooms">Мої кімнати</Link>
          </div>
        ) : user !== null ? (
          <RecommendationResults key={`${validRoomId}:${user.id}`} roomId={validRoomId} userId={user.id}/>
        ) : null}
      </main>
      <PageFooter/>
    </div>
  )
}
