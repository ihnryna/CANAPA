import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { MoviePreferences } from '../components/MoviePreferences'
import { MovieBrowser } from '../components/MovieBrowser'
import type { Movie } from '../api/movies'
import './MovieDiscoveryPage.css'
import { isUuid } from "../auth/user-id.ts";
import { AppHeader } from "../components/AppHeader.tsx";
import { PageFooter } from "../components/PageFooter.tsx";
import { Icon } from "../components/Icon.tsx";

interface MovieDiscoveryPageProps {
  userId: string
}

export function MovieDiscoveryPage({ userId }: MovieDiscoveryPageProps) {
  const { roomId } = useParams<{ roomId: string }>()
  const validRoomId = roomId !== undefined && isUuid(roomId) ? roomId : undefined
  const [selectedMovies, setSelectedMovies] = useState<Movie[]>([])
  const [activeTab, setActiveTab] = useState<'preferences' | 'movies'>(
    'preferences',
  )

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



  function toggleMovie(movie: Movie) {
    setSelectedMovies((current) =>
      current.some((item) => item.id === movie.id)
        ? current.filter((item) => item.id !== movie.id)
        : [...current, movie],
    )
  }

  return (
    <main className="main-content movie-discovery-page">
      <Link className="back-link" to={`/rooms/${roomId}`}>
        <Icon name="back" size={16} />
        Повернутися до кімнати
      </Link>

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
        <MoviePreferences roomId={validRoomId} userId={userId}/>
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
