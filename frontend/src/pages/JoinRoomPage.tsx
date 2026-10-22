import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiClientError } from '../api/client'
import { validateInvitation, type Invitation } from '../api/invitations'
import { getRoom, joinRoom, type Room } from '../api/rooms'
import { useCurrentUser } from '../auth/current-user-context'
import { AppHeader } from '../components/AppHeader'
import { Icon } from '../components/Icon'
import { PageFooter } from '../components/PageFooter'

function isInvitationCode(value: string): boolean {
  return /^[A-Za-z0-9_-]{1,64}$/.test(value)
}

function messageFor(error: unknown): string {
  if (error instanceof ApiClientError) {
    if (error.status === 404) return 'Запрошення не знайдено.'
    if (error.status === 410) return 'Строк дії запрошення завершився.'
    return error.message
  }
  return error instanceof Error ? error.message : 'Не вдалося перевірити запрошення.'
}

export function JoinRoomPage() {
  const { code = '' } = useParams()
  const decodedCode = useMemo(() => {
    try {
      return decodeURIComponent(code)
    } catch {
      return ''
    }
  }, [code])
  const navigate = useNavigate()
  const { user, status, restoreError, retryRestore } = useCurrentUser()
  const [invitation, setInvitation] = useState<Invitation | null>(null)
  const [room, setRoom] = useState<Room | null>(null)
  const [loading, setLoading] = useState(false)
  const [joining, setJoining] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [retryToken, setRetryToken] = useState(0)

  useEffect(() => {
    if (status !== 'ready' || user === null || !isInvitationCode(decodedCode)) return

    const controller = new AbortController()
    let active = true
    const loadInvitation = async () => {
      setLoading(true)
      setError(null)
      setInvitation(null)
      setRoom(null)
      try {
        const validated = await validateInvitation(decodedCode, user.id, controller.signal)
        if (validated.status === 'EXPIRED') throw new Error('Строк дії запрошення завершився.')
        const loadedRoom = await getRoom(validated.roomId, controller.signal)
        if (!active) return
        setInvitation(validated)
        setRoom(loadedRoom)
      } catch (caught: unknown) {
        if (!active || controller.signal.aborted) return
        setError(messageFor(caught))
      } finally {
        if (active) setLoading(false)
      }
    }
    void loadInvitation()

    return () => {
      active = false
      controller.abort()
    }
  }, [decodedCode, retryToken, status, user])

  async function handleJoin() {
    if (user === null || room === null) return
    setJoining(true)
    setError(null)
    try {
      await joinRoom(room.id, user.id)
      navigate(`/rooms/${room.id}`, { replace: true, state: { joined: true } })
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.code === 'ROOM_PARTICIPANT_ALREADY_EXISTS') {
        navigate(`/rooms/${room.id}`, { replace: true, state: { alreadyMember: true } })
        return
      }
      setError(messageFor(caught))
    } finally {
      setJoining(false)
    }
  }

  const alreadyMember = room?.participants.some((member) => member.id === user?.id) ?? false
  const canJoin = room?.status === 'CREATED' && !alreadyMember
  const returnTo = `/join/${encodeURIComponent(decodedCode)}`
  const invalidCode = !isInvitationCode(decodedCode)

  return (
    <div className="app-shell">
      <AppHeader/>
      <main className="main-content invitation-page">
        <Link className="back-link" to="/rooms"><Icon name="back" size={16}/> До моїх кімнат</Link>
        <section className="invitation-card">
          <span className="form-symbol"><Icon name="link" size={26}/></span>
          <div className="eyebrow">ЗАПРОШЕННЯ ДО CANAPA</div>
          <h1>Вас запросили на кіновечір</h1>

          {invalidCode ? (
            <div className="inline-alert" role="alert"><h2>Некоректне посилання</h2><p>Адреса не містить дійсного
              invitation code.</p><Link className="btn secondary" to="/rooms">До моїх кімнат</Link></div>
          ) : status === 'loading' || loading ? (
            <div className="inline-loading" aria-busy="true"><span className="spinner"/><p>Перевіряємо запрошення…</p>
            </div>
          ) : status === 'error' ? (
            <div className="inline-alert" role="alert"><p>{restoreError}</p>
              <button className="btn secondary" onClick={retryRestore}>Спробувати ще раз</button>
            </div>
          ) : user === null ? (
            <div className="invitation-profile-needed">
              <p>Щоб безпечно перевірити запрошення та приєднатися, спочатку створіть або відновіть профіль.</p>
              <Link className="btn primary" to={`/?returnTo=${encodeURIComponent(returnTo)}`}>Налаштувати профіль <Icon
                name="arrow" size={17}/></Link>
              <small>Код запрошення збережено в адресі — після профілю ви повернетеся сюди.</small>
            </div>
          ) : error !== null && room === null ? (
            <div className="inline-alert" role="alert">
              <h2>Не вдалося відкрити запрошення</h2><p>{error}</p>
              <div className="actions">
                <button className="btn primary" onClick={() => setRetryToken((value) => value + 1)}>Спробувати ще раз
                </button>
                <Link className="btn secondary" to="/rooms">До моїх кімнат</Link></div>
            </div>
          ) : room !== null && invitation !== null ? (
            <>
              <div className="invitation-details">
                <span
                  className={`status-badge status-${room.status.toLowerCase()}`}>{room.status === 'CREATED' ? 'Підготовка' : room.status === 'VOTING' ? 'Голосування' : 'Завершено'}</span>
                <h2>{room.name}</h2>
                <p>Організатор: <strong>{room.creator.name}</strong></p>
                <p>{room.participants.length} учасників</p>
                <p>Запрошення діє до {new Intl.DateTimeFormat('uk-UA', {
                  dateStyle: 'medium',
                  timeStyle: 'short'
                }).format(new Date(invitation.expiresAt))}</p>
                <div className="joining-as"><span className="avatar">{user.name.charAt(0).toUpperCase()}</span><span>Ви приєднаєтеся як <strong>{user.name}</strong></span>
                </div>
              </div>
              {alreadyMember &&
                  <p className="success-message" role="status"><Icon name="check" size={17}/> Ви вже є учасником цієї
                      кімнати.</p>}
              {room.status !== 'CREATED' && !alreadyMember &&
                  <p className="form-error" role="alert">До кімнати не можна приєднатися після початку голосування.</p>}
              {error !== null && <p className="form-error" role="alert">{error}</p>}
              <div className="form-actions">
                <Link className="btn secondary" to="/rooms">Не зараз</Link>
                {alreadyMember ?
                  <Link className="btn primary" to={`/rooms/${room.id}`}>Відкрити кімнату <Icon name="arrow" size={17}/></Link> :
                  <button className="btn primary" onClick={handleJoin}
                          disabled={!canJoin || joining}>{joining ? 'Приєднуємо…' : 'Приєднатися до кімнати'} <Icon
                    name="arrow" size={17}/></button>}
              </div>
            </>
          ) : null}
        </section>
      </main>
      <PageFooter/>
    </div>
  )
}
