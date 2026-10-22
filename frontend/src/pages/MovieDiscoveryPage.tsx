import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { MoviePreferences } from '../components/MoviePreferences'
import type { Movie } from '../api/movies'
import './MovieDiscoveryPage.css'
import { isUuid } from "../auth/user-id.ts";
import { AppHeader } from "../components/AppHeader.tsx";
import { PageFooter } from "../components/PageFooter.tsx";
import { RoomPageHeader } from "../components/RoomPageHeader.tsx";
import { useRoomPolling } from "../hooks/useRoomPolling.ts";
import { useCurrentUser } from "../auth/current-user-context.ts";

interface MovieDiscoveryPageProps {
  userId: string
}

const STATUS_LABELS = {
  CREATED: 'Підготовка',
  VOTING: 'Голосування триває',
  FINISHED: 'Завершено',
} as const

export function MovieDiscoveryPage({ userId }: MovieDiscoveryPageProps) {
  const { user } = useCurrentUser()
  const { roomId } = useParams<{ roomId: string }>()
  const validRoomId = roomId !== undefined && isUuid(roomId) ? roomId : undefined
  const [selectedMovies, setSelectedMovies] = useState<Movie[]>([])
  const { room, error, lastUpdatedAt, refresh } = useRoomPolling(validRoomId)
  const location = useLocation()
  const routeState = location.state as { created?: boolean; joined?: boolean; alreadyMember?: boolean } | null
  const initialNotice = routeState?.created ? 'Кімнату створено' : routeState?.joined ? 'Ви приєдналися до кімнати' : routeState?.alreadyMember ? 'Ви вже були учасником кімнати' : null
  const [, setNotice] = useState<string | null>(initialNotice)
  const navigate = useNavigate()
  const member = room?.participants.some((participant) => participant.id === user?.id) ?? false

  if (room === null)
    return

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

  async function copyRoomId() {
    if (room === null) return
    try {
      await navigator.clipboard.writeText(room.id)
      setNotice('ID кімнати скопійовано')
    } catch {
      setNotice('Не вдалося скопіювати ID')
    }
  }

  return (
    <>
      <AppHeader/>
      <main className="main-content movie-discovery-page">
        <RoomPageHeader
          error={error}
          refresh={refresh}
          member={member}
          room={room}
          STATUS_LABELS={STATUS_LABELS}
          copyRoomId={copyRoomId}
          lastUpdatedAt={lastUpdatedAt}
          navigate={navigate}
          currentIdx={1}
        />

        <MoviePreferences roomId={validRoomId} userId={userId}/>
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
    </>
  )
}
