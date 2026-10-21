import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getRooms, type Room } from '../api/rooms'
import { useCurrentUser } from '../auth/current-user-context'
import { AppHeader } from '../components/AppHeader'
import { Icon } from '../components/Icon'
import { PageFooter } from '../components/PageFooter'

const STATUS_LABELS = {
  CREATED: 'Підготовка',
  VOTING: 'Голосування',
  FINISHED: 'Завершено',
} as const

function roomCountLabel(count: number): string {
  const lastTwoDigits = count % 100
  if (lastTwoDigits >= 11 && lastTwoDigits <= 14) return 'кімнат'
  if (count % 10 === 1) return 'кімната'
  if (count % 10 >= 2 && count % 10 <= 4) return 'кімнати'
  return 'кімнат'
}

function invitationCode(value: string): string | null {
  const trimmed = value.trim()
  if (!trimmed) return null

  let candidate: string
  try {
    const url = new URL(trimmed)
    const pathMatch = url.pathname.match(/\/join\/([^/]+)/)
    candidate = pathMatch?.[1] || url.searchParams.get('invite') || ''
  } catch {
    const pathMatch = trimmed.match(/(?:^|\/)join\/([^/?#]+)/)
    candidate = pathMatch?.[1] || trimmed
  }

  try {
    candidate = decodeURIComponent(candidate)
  } catch {
    return null
  }
  return /^[A-Za-z0-9_-]{1,64}$/.test(candidate) ? candidate : null
}

export function RoomsPage() {
  const navigate = useNavigate()
  const { user } = useCurrentUser()
  const [showJoin, setShowJoin] = useState(false)
  const [joinValue, setJoinValue] = useState('')
  const [joinError, setJoinError] = useState<string | null>(null)
  const [rooms, setRooms] = useState<Room[]>([])
  const [loadingRooms, setLoadingRooms] = useState(true)
  const [roomsError, setRoomsError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    if (user === null) return
    const controller = new AbortController()

    getRooms(user.id, controller.signal)
      .then(setRooms)
      .catch((caught: unknown) => {
        if (controller.signal.aborted) return
        setRoomsError(caught instanceof Error ? caught.message : 'Не вдалося завантажити кімнати.')
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoadingRooms(false)
      })

    return () => controller.abort()
  }, [reloadKey, user])

  function handleJoin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const code = invitationCode(joinValue)
    if (code === null) {
      setJoinError('Вставте коректне посилання-запрошення або код.')
      return
    }
    navigate(`/join/${encodeURIComponent(code)}`)
  }

  return (
    <div className="app-shell">
      <AppHeader/>
      <main className="main-content">
        <section className="rooms-heading">
          <div className="page-heading">
            <div className="eyebrow">ВАШ ПРОСТІР ДЛЯ КІНО</div>
            <h1>Мої кімнати</h1>
            <p>Привіт, {user?.name.split(' ')[0]}! Створіть новий кіновечір або приєднайтеся до друзів.</p>
          </div>
          <div className="actions">
            <button className="btn secondary" onClick={() => setShowJoin(true)}><Icon name="link"/> Приєднатися</button>
            <Link className="btn primary" to="/rooms/new"><Icon name="plus"/> Створити кімнату <Icon name="arrow"
                                                                                                     size={17}/></Link>
          </div>
        </section>

        <section className="dashboard-banner">
          <div>
            <span className="eyebrow">КІНОВЕЧІР ПОЧИНАЄТЬСЯ ТУТ</span>
            <h2>Ваша компанія.<br/>Ваш спільний вибір.</h2>
            <p>Кіновечір без нескінченних суперечок.</p>
          </div>
          <div className="banner-art" aria-hidden="true"><Icon name="film" size={76}/><span/><span/></div>
        </section>

        <section className="rooms-list-section" aria-labelledby="rooms-list-title" aria-busy={loadingRooms}>
          <div className="section-heading rooms-list-heading">
            <div>
              <div className="eyebrow">ІСТОРІЯ КІМНАТ</div>
              <h2 id="rooms-list-title">Ваші кіновечори</h2></div>
            {!loadingRooms && roomsError === null && rooms.length > 0 &&
                <span className="badge">{rooms.length} {roomCountLabel(rooms.length)}</span>}
          </div>

          {loadingRooms ? (
            <div className="rooms-loading"><span className="spinner"/><p>Завантажуємо кімнати…</p></div>
          ) : roomsError !== null ? (
            <div className="rooms-unavailable" role="alert">
              <span className="state-symbol">!</span>
              <div><h3>Не вдалося завантажити кімнати</h3><p>{roomsError}</p></div>
              <button className="btn secondary" onClick={() => {
                setLoadingRooms(true);
                setRoomsError(null);
                setReloadKey((value) => value + 1)
              }}>Спробувати ще раз
              </button>
            </div>
          ) : rooms.length === 0 ? (
            <div className="rooms-unavailable">
              <span className="state-symbol"><Icon name="film" size={28}/></span>
              <div><h3>У вас ще немає кімнат</h3><p>Створіть свій перший кіновечір або приєднайтеся за запрошенням
                друзів.</p></div>
              <div className="actions"><Link className="btn primary" to="/rooms/new">Створити кімнату <Icon name="arrow"
                                                                                                            size={17}/></Link>
                <button className="text-button" onClick={() => setShowJoin(true)}>У мене є запрошення</button>
              </div>
            </div>
          ) : (
            <div className="rooms-grid">
              {rooms.map((room) => {
                const isOrganizer = room.creator.id === user?.id
                return (
                  <Link className="room-card" to={`/rooms/${room.id}`} key={room.id}>
                    <div className="room-card-top"><span
                      className={`status-badge status-${room.status.toLowerCase()}`}>{STATUS_LABELS[room.status]}</span><Icon
                      name="arrow" size={18}/></div>
                    <h3>{room.name}</h3>
                    <p>{isOrganizer ? 'Ви організатор' : `Організатор: ${room.creator.name}`}</p>
                    <div className="room-card-meta"><span><Icon name="users"
                                                                size={16}/> {room.participants.length}</span>
                      <time dateTime={room.createdAt}>{new Intl.DateTimeFormat('uk-UA', {
                        day: 'numeric',
                        month: 'short',
                        year: 'numeric'
                      }).format(new Date(room.createdAt))}</time>
                    </div>
                  </Link>
                )
              })}
            </div>
          )}
        </section>
      </main>
      <PageFooter/>

      {showJoin && (
        <div className="modal-backdrop"
             onMouseDown={(event) => event.target === event.currentTarget && setShowJoin(false)}>
          <section className="modal" role="dialog" aria-modal="true" aria-labelledby="join-dialog-title">
            <button className="modal-close" aria-label="Закрити" onClick={() => setShowJoin(false)}><Icon name="close"/>
            </button>
            <span className="form-symbol"><Icon name="link" size={25}/></span>
            <h2 id="join-dialog-title">Приєднатися за запрошенням</h2>
            <p>Вставте повне посилання або invitation code.</p>
            <form onSubmit={handleJoin}>
              <label>Посилання-запрошення<input autoFocus value={joinValue} onChange={(event) => {
                setJoinValue(event.target.value);
                setJoinError(null)
              }} placeholder="https://…/join/code або code"/></label>
              {joinError !== null && <p className="form-error" role="alert">{joinError}</p>}
              <div className="modal-actions">
                <button type="button" className="btn secondary" onClick={() => setShowJoin(false)}>Не зараз</button>
                <button className="btn primary">Перевірити <Icon name="arrow" size={17}/></button>
              </div>
            </form>
          </section>
        </div>
      )}
    </div>
  )
}
