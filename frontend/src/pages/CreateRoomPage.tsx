import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiClientError } from '../api/client'
import { createRoom } from '../api/rooms'
import { useCurrentUser } from '../auth/current-user-context'
import { AppHeader } from '../components/AppHeader'
import { Icon } from '../components/Icon'
import { PageFooter } from '../components/PageFooter'

export function CreateRoomPage() {
  const navigate = useNavigate()
  const { user } = useCurrentUser()
  const [name, setName] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalized = name.trim()
    if (!normalized) {
      setError('Введіть назву кімнати.')
      return
    }
    if (normalized.length > 100) {
      setError('Назва не може перевищувати 100 символів.')
      return
    }
    if (user === null) return

    setPending(true)
    setError(null)
    try {
      const room = await createRoom(normalized, user.id)
      navigate(`/rooms/${room.id}`, { replace: true, state: { created: true } })
    } catch (caught) {
      setError(caught instanceof ApiClientError || caught instanceof Error ? caught.message : 'Не вдалося створити кімнату.')
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="app-shell">
      <AppHeader/>
      <main className="main-content narrow-page">
        <Link className="back-link" to="/rooms"><Icon name="back" size={16}/> Мої кімнати</Link>
        <section className="create-room-card">
          <span className="form-symbol"><Icon name="plus" size={27}/></span>
          <div className="eyebrow">НОВИЙ КІНОВЕЧІР</div>
          <h1>Створити кімнату</h1>
          <p>Ви станете організатором і зможете запросити друзів після створення.</p>
          <form onSubmit={handleSubmit} aria-busy={pending}>
            <label>Назва кімнати<input autoFocus value={name} onChange={(event) => {
              setName(event.target.value);
              setError(null)
            }} maxLength={100} placeholder="Наприклад, П’ятничний кіновечір" disabled={pending}/></label>
            <div className="field-meta">
              <span>Назву можна змінити лише після появи відповідного API.</span><span>{name.length} / 100</span></div>
            {error !== null && <p className="form-error" role="alert">{error}</p>}
            <div className="form-actions"><Link className="btn secondary" to="/rooms">Скасувати</Link>
              <button className="btn primary" disabled={pending}>{pending ? 'Створюємо…' : 'Створити кімнату'} <Icon
                name="arrow" size={17}/></button>
            </div>
          </form>
        </section>
      </main>
      <PageFooter/>
    </div>
  )
}
