import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { createInvitation, type Invitation } from '../api/invitations'
import { useCurrentUser } from '../auth/current-user-context'
import { isUuid } from '../auth/user-id'
import { AppHeader } from '../components/AppHeader'
import { Icon } from '../components/Icon'
import { PageFooter } from '../components/PageFooter'
import { useRoomPolling } from '../hooks/useRoomPolling'

const STATUS_LABELS = {
  CREATED: 'Підготовка',
  VOTING: 'Голосування триває',
  FINISHED: 'Завершено',
} as const

interface InvitationDialogProps {
  roomId: string
  userId: string
  onClose: () => void
  onNotice: (message: string) => void
}

function InvitationDialog({ roomId, userId, onClose, onNotice }: InvitationDialogProps) {
  const [expiresInHours, setExpiresInHours] = useState(24)
  const [invitation, setInvitation] = useState<Invitation | null>(null)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const closeButtonRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    closeButtonRef.current?.focus()
    const onKeyDown = (event: KeyboardEvent) => event.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPending(true)
    setError(null)
    try {
      setInvitation(await createInvitation(roomId, userId, expiresInHours))
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Не вдалося створити запрошення.')
    } finally {
      setPending(false)
    }
  }

  async function copyLink() {
    if (invitation === null) return
    try {
      await navigator.clipboard.writeText(invitation.link)
      onNotice('Посилання скопійовано')
    } catch {
      setError('Автоматичне копіювання недоступне. Скопіюйте посилання з поля.')
    }
  }

  async function shareLink() {
    if (invitation === null || navigator.share === undefined) return
    try {
      await navigator.share({
        title: 'Запрошення до CANAPA',
        text: 'Приєднуйся до нашого кіновечора',
        url: invitation.link
      })
    } catch (caught) {
      if (!(caught instanceof DOMException && caught.name === 'AbortError')) setError('Не вдалося відкрити системне меню поширення.')
    }
  }

  return (
    <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="modal" role="dialog" aria-modal="true" aria-labelledby="invite-dialog-title">
        <button ref={closeButtonRef} className="modal-close" aria-label="Закрити" onClick={onClose}><Icon name="close"/>
        </button>
        <span className="form-symbol"><Icon name="link" size={25}/></span>
        <h2 id="invite-dialog-title">Запросити друзів</h2>
        <p>Створіть посилання для людей, з якими хочете обрати фільм.</p>
        <form onSubmit={handleCreate}>
          <label>Строк дії запрошення<select value={expiresInHours} onChange={(event) => {
            setExpiresInHours(Number(event.target.value));
            setInvitation(null)
          }} disabled={pending}>
            <option value={1}>1 година</option>
            <option value={24}>24 години</option>
            <option value={72}>3 дні</option>
            <option value={168}>7 днів</option>
          </select></label>
          {invitation !== null && <label>Посилання-запрошення<input readOnly value={invitation.link}
                                                                    onFocus={(event) => event.currentTarget.select()}/></label>}
          {error !== null && <p className="form-error" role="alert">{error}</p>}
          <div className="modal-actions">
            {invitation === null ?
              <button className="btn primary" disabled={pending}>{pending ? 'Створюємо…' : 'Створити посилання'} <Icon
                name="arrow" size={17}/></button> : <>
                <button type="button" className="btn primary" onClick={copyLink}><Icon name="copy" size={17}/> Копіювати
                </button>
                {navigator.share !== undefined &&
                    <button type="button" className="btn secondary" onClick={shareLink}>Поділитися</button>}</>}
          </div>
        </form>
      </section>
    </div>
  )
}

export function RoomPage() {
  const navigate = useNavigate()
  const { roomId } = useParams()
  const validRoomId = roomId !== undefined && isUuid(roomId) ? roomId : undefined
  const { user } = useCurrentUser()
  const location = useLocation()
  const routeState = location.state as { created?: boolean; joined?: boolean; alreadyMember?: boolean } | null
  const initialNotice = routeState?.created ? 'Кімнату створено' : routeState?.joined ? 'Ви приєдналися до кімнати' : routeState?.alreadyMember ? 'Ви вже були учасником кімнати' : null
  const [notice, setNotice] = useState<string | null>(initialNotice)
  const [showInvitation, setShowInvitation] = useState(false)
  const { room, readyUserIds, loading, error, preferencesError, lastUpdatedAt, refresh } = useRoomPolling(validRoomId)

  useEffect(() => {
    if (notice === null) return
    const timer = window.setTimeout(() => setNotice(null), 3200)
    return () => window.clearTimeout(timer)
  }, [notice])

  const organizer = room?.creator.id === user?.id
  const member = room?.participants.some((participant) => participant.id === user?.id) ?? false
  const readyCount = useMemo(() => room === null || readyUserIds === null ? null : room.participants.filter((participant) => readyUserIds.has(participant.id)).length, [readyUserIds, room])

  async function copyRoomId() {
    if (room === null) return
    try {
      await navigator.clipboard.writeText(room.id)
      setNotice('ID кімнати скопійовано')
    } catch {
      setNotice('Не вдалося скопіювати ID')
    }
  }

  if (validRoomId === undefined) {
    return <div className="app-shell"><AppHeader/>
      <main className="centered-state"><span className="state-symbol">!</span><h1>Некоректний ID кімнати</h1><p>Адреса
        має містити UUID кімнати.</p><Link className="btn primary" to="/rooms">До моїх кімнат</Link></main>
    </div>
  }

  if (loading && room === null) {
    return <div className="app-shell"><AppHeader/>
      <main className="centered-state" aria-busy="true"><span className="spinner"/><h1>Відкриваємо кімнату</h1>
        <p>Завантажуємо учасників і поточний статус.</p></main>
    </div>
  }

  if (room === null) {
    return <div className="app-shell"><AppHeader/>
      <main className="centered-state"><span className="state-symbol">!</span><h1>Кімнату не вдалося відкрити</h1><p
        role="alert">{error}</p>
        <div className="actions">
          <button className="btn primary" onClick={() => void refresh()}>Спробувати ще раз</button>
          <Link className="btn secondary" to="/rooms">Назад</Link></div>
      </main>
    </div>
  }

  return (
    <div className="app-shell">
      <AppHeader/>
      <main className="main-content">
        <Link className="back-link" to="/rooms"><Icon name="back" size={16}/> Мої кімнати</Link>
        {error !== null && <div className="polling-alert" role="alert"><span>Оновлення призупинено: {error}</span>
            <button className="text-button" onClick={() => void refresh()}>Повторити</button>
        </div>}
        {!member && <div className="polling-alert warning" role="alert">Ваш профіль не входить до списку учасників. Дані
            доступні через backend, але дії кімнати приховано.</div>}

        <section className="room-top">
          <div>
            <div className="eyebrow">ВАША КІНОКОМПАНІЯ</div>
            <h1>{room.name}</h1></div>
          <span className={`status-badge status-${room.status.toLowerCase()}`}>{STATUS_LABELS[room.status]}</span>
        </section>
        <div className="room-meta"><span>ID: <code>{room.id}</code></span>
          <button className="text-button compact" onClick={copyRoomId}><Icon name="copy" size={15}/> Копіювати ID
          </button>
          <span className="polling-note">{lastUpdatedAt === null ? 'Оновлюємо…' : 'Автоматично оновлюється'}</span>
        </div>
        {room.status === 'VOTING' && (
          <div className="voting-entry">
            <Link className="btn primary" to={`/rooms/${room.id}/voting`}>
              Перейти до голосування
              <Icon name="arrow" size={17}/>
            </Link>
          </div>
        )}
        <nav className="room-steps" aria-label="Етапи кімнати">
          {[
            'Учасники',
            'Уподобання',
            'Голосування',
            'Результат',
          ].map((step, index) => (
            <button
              key={step}
              type="button"
              className={index === 0 ? 'active' : ''}
              disabled={!member}
              onClick={() => {
                if (index === 1) {
                  navigate(`/rooms/${room.id}/discover`)
                }
                if (index === 2) {
                  navigate(`/rooms/${room.id}/voting`)
                }
              }}
            >
              <span>0{index + 1}</span>
              {step}
            </button>
          ))}
        </nav>

        <section className="rooms-heading lobby-heading">
          <div className="page-heading">
            <div className="eyebrow">ЗБИРАЄМО КОМПАНІЮ</div>
            <h2>{organizer ? 'Гарне кіно починається з вас' : 'Ваша компанія вже збирається'}</h2>
            <p>{organizer ? 'Ви — організатор. Запросіть друзів і стежте за готовністю групи.' : 'Ви — учасник. Сторінка автоматично показуватиме зміни в кімнаті.'}</p>
          </div>
          {organizer && member && room.status === 'CREATED' &&
              <button className="btn secondary" onClick={() => setShowInvitation(true)}><Icon name="link"/> Запросити
                  друзів</button>}
        </section>

        <div className="lobby-grid">
          <section className="panel members-panel">
            <div className="section-heading"><h3>Учасники</h3>{readyCount === null ?
              <span className="badge">Готовність невідома</span> :
              <span className="badge">{readyCount} із {room.participants.length} готові</span>}</div>
            <div className="member-list">
              {room.participants.map((participant, index) => {
                const isReady = readyUserIds?.has(participant.id)
                return <div className="person" key={participant.id}><span
                  className={`avatar avatar-${index % 4}`}>{participant.name.charAt(0).toUpperCase()}</span>
                  <div>
                    <strong>{participant.name}</strong><small>{participant.id === room.creator.id ? 'Організатор' : participant.id === user?.id ? 'Ви' : 'Учасник'}</small>
                  </div>
                  <span
                    className={`person-status ${isReady ? 'is-ready' : ''}`}>{readyUserIds === null ? 'Невідомо' : isReady ? <>
                    <Icon name="check" size={15}/> Готовий</> : 'Ще обирає'}</span></div>
              })}
            </div>
            {preferencesError &&
                <p className="subtle-note">Backend кімнати доступний, але статуси вподобань зараз не вдалося
                    отримати.</p>}
          </section>

          <div className="lobby-side">
            <section className="panel preference-card">
              <span className="form-symbol"><Icon name="check" size={24}/></span>
              <h3>Ваші вподобання</h3>
              <p>{readyUserIds === null ? 'Статус уподобань недоступний' : readyUserIds.has(user?.id ?? '') ? 'Уподобання збережено' : 'Ви ще не заповнили вподобання'}</p>
              <button className="btn primary" onClick={() => navigate(`/rooms/${room.id}/discover`)}>Додати уподобання</button>
            </section>
            <section className="panel room-summary">
              <h3>Усе для кіновечора</h3>
              <p><Icon name="users"/> {room.participants.length} учасників</p>
              <p><Icon name="film"/> Статус: {STATUS_LABELS[room.status]}</p>
              <p className="subtle-note">
                Перейдіть до вкладки «Голосування», щоб переглянути
                рекомендації фільмів для вашої компанії.
              </p>
            </section>
          </div>
        </div>

        <div className="bottom-bar">
          <p>{room.status === 'CREATED' ? 'Кімната відкрита для нових учасників.' : room.status === 'VOTING' ? 'Голосування вже почалося. Екран голосування буде додано окремо.' : 'Кіновечір завершено. Екран результату буде додано окремо.'}</p>
          <button className="btn secondary" onClick={() => void refresh()}>Оновити зараз</button>
        </div>
      </main>
      <PageFooter/>
      {showInvitation && user !== null &&
          <InvitationDialog roomId={room.id} userId={user.id} onClose={() => setShowInvitation(false)}
                            onNotice={(message) => setNotice(message)}/>}
      {notice !== null && <div className="toast" role="status"><Icon name="check" size={18}/> {notice}</div>}
    </div>
  )
}
