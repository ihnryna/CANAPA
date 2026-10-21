import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { MoviePreferences } from '../components/MoviePreferences'
import { MovieBrowser } from '../components/MovieBrowser'
import type { Movie } from '../api/movies'
import './MovieDiscoveryPage.css'

interface MovieDiscoveryPageProps {
  userId: string
}

export function MovieDiscoveryPage({ userId }: MovieDiscoveryPageProps) {
  const { roomId } = useParams<{ roomId: string }>()
  const [selectedMovies, setSelectedMovies] = useState<Movie[]>([])
  const [activeTab, setActiveTab] = useState<'preferences' | 'movies'>(
    'preferences',
  )

  if (!roomId) {
    return (
      <main className="main-content">
        <div className="inline-alert">
          <h2>Кімнату не знайдено</h2>
          <p>Перевірте посилання та спробуйте ще раз.</p>
        </div>
      </main>
    )
  }

  function toggleMovie(movie: Movie) {
    setSelectedMovies((current) =>
      current.some((item) => item.id === movie.id)
        ? current.filter((item) => item.id !== movie.id)
        : [...current, movie],
    )
  }

  return (
    <main className="main-content movie-discovery-page">
      <div className="page-heading">
        <span className="eyebrow">
          <span className="tiny-line"/>
          CANAPA / DISCOVER
        </span>
        <h1>Знайдіть фільм для вашої компанії</h1>
        <p>
          Налаштуйте вподобання та перегляньте фільми, які можуть
          сподобатися саме вам.
        </p>
      </div>

      <div className="discovery-tabs" role="tablist" aria-label="Розділи">
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'preferences'}
          className={activeTab === 'preferences' ? 'active' : ''}
          onClick={() => setActiveTab('preferences')}
        >
          01. Вподобання
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'movies'}
          className={activeTab === 'movies' ? 'active' : ''}
          onClick={() => setActiveTab('movies')}
        >
          02. Пошук фільмів
        </button>
      </div>

      {activeTab === 'preferences' ? (
        <MoviePreferences roomId={roomId} userId={userId}/>
      ) : (
        <MovieBrowser
          onSelectMovie={toggleMovie}
          selectedMovieIds={selectedMovies.map((movie) => movie.id)}
        />
      )}

      {selectedMovies.length > 0 && (
        <section className="selected-movies-panel">
          <div>
            <span className="eyebrow">YOUR SELECTION</span>
            <h2>Обрані фільми ({selectedMovies.length})</h2>
          </div>

          <ul>
            {selectedMovies.map((movie) => (
              <li key={movie.id}>
                <span>{movie.title}</span>
                <button
                  type="button"
                  aria-label={`Видалити фільм ${movie.title}`}
                  onClick={() => toggleMovie(movie)}
                >
                  Видалити
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}
    </main>
  )
}
