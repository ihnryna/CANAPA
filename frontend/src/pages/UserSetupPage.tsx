import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { ApiClientError } from '../api/client'
import { createUser, getUserByEmail } from '../api/users'
import { useCurrentUser } from '../auth/current-user-context'
import { AppHeader } from '../components/AppHeader'
import { Icon } from '../components/Icon'
import { PageFooter } from '../components/PageFooter'

type PendingAction = 'create' | 'select' | null

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function errorMessage(error: unknown): string {
  if (error instanceof ApiClientError || error instanceof Error) {
    return error.message
  }
  return 'Сталася помилка. Спробуйте ще раз.'
}

function safeReturnTo(value: string | null): string {
  if (value?.startsWith('/') && !value.startsWith('//')) return value
  return '/setup'
}

export function UserSetupPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const destination = safeReturnTo(searchParams.get('returnTo'))
  const { user, status, restoreError, selectUser, clearUser, retryRestore } = useCurrentUser()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [existingUserEmail, setExistingUserEmail] = useState('')
  const [pendingAction, setPendingAction] = useState<PendingAction>(null)
  const [createError, setCreateError] = useState<string | null>(null)
  const [selectError, setSelectError] = useState<string | null>(null)

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setCreateError(null)
    const normalizedName = name.trim()
    const normalizedEmail = email.trim()
    if (!normalizedName) {
      setCreateError('Введіть ім’я.')
      return
    }
    if (normalizedName.length > 100) {
      setCreateError('Ім’я не може перевищувати 100 символів.')
      return
    }
    if (!normalizedEmail || normalizedEmail.length > 320 || !EMAIL_PATTERN.test(normalizedEmail)) {
      setCreateError('Введіть коректний email.')
      return
    }

    setPendingAction('create')
    try {
      const createdUser = await createUser({ name: normalizedName, email: normalizedEmail })
      selectUser(createdUser)
      navigate(destination, { replace: true })
    } catch (error) {
      setCreateError(errorMessage(error))
    } finally {
      setPendingAction(null)
    }
  }

  async function handleSelect(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSelectError(null)
    const normalizedEmail = existingUserEmail.trim()
    if (!normalizedEmail || normalizedEmail.length > 320 || !EMAIL_PATTERN.test(normalizedEmail)) {
      setSelectError('Введіть коректний email.')
      return
    }

    setPendingAction('select')
    try {
      const selectedUser = await getUserByEmail(normalizedEmail)
      selectUser(selectedUser)
      navigate(destination, { replace: true })
    } catch (error) {
      setSelectError(
        error instanceof ApiClientError && error.code === 'USER_NOT_FOUND'
          ? 'Профіль із таким email не знайдено.'
          : errorMessage(error),
      )
    } finally {
      setPendingAction(null)
    }
  }

  if (status === 'loading') {
    return <main className="centered-state" aria-busy="true"><span className="spinner" /><h1>Відновлюємо профіль</h1></main>
  }

  return (
    <div className="app-shell">
      <AppHeader />
      <main className="onboarding">
        <section className="welcome">
          <div className="eyebrow"><span className="tiny-line" /> ВАШ НАСТУПНИЙ КІНОВЕЧІР</div>
          <h1>Знайдіть фільм,<br />який захочуть<br />подивитися <em>всі.</em></h1>
          <p className="welcome-description">Створіть профіль, щоб почати спільний вибір фільму.</p>
          <div className="cinema-card" aria-hidden="true">
            <span className="cinema-reel"><Icon name="film" size={28} /></span>
            <p>Кіновечір без<br /><strong>нескінченних суперечок.</strong></p>
          </div>
          <div className="welcome-bottom"><span>Гарне кіно. Ваша компанія.</span><span>Один спільний вибір.</span></div>
        </section>

        <section className="profile-side" aria-label="Налаштування профілю">
          <div className="onboard-steps" aria-label="Етапи CANAPA">
            {['Профіль', 'Кімната', 'Вибір'].map((label, index) => (
              <div className={index === 0 ? 'current' : ''} key={label}><span>0{index + 1}</span>{label}{index < 2 && <i />}</div>
            ))}
          </div>

          <div className="profile-form">
            {status === 'error' && (
              <div className="inline-alert" role="alert">
                <p>{restoreError}</p>
                <button className="text-button" onClick={retryRestore}>Спробувати ще раз</button>
              </div>
            )}

            {user !== null ? (
              <div className="returning-profile">
                <span className="form-symbol"><Icon name="check" size={26} /></span>
                <h2>Раді бачити знову</h2>
                <p>Зараз вибрано</p>
                <div className="saved-profile"><span className="avatar">{user.name.charAt(0).toUpperCase()}</span><div><strong>{user.name}</strong><small>{user.email}</small></div></div>
                <button className="btn primary full" onClick={() => navigate(destination)}>Продовжити <Icon name="arrow" size={18} /></button>
                <button className="text-button centered" onClick={clearUser}>Змінити користувача</button>
              </div>
            ) : (
              <>
                <span className="form-symbol"><Icon name="users" size={26} /></span>
                <h2>Створити профіль</h2>
                <p>Це займе лише кілька секунд</p>
                <form onSubmit={handleCreate} aria-busy={pendingAction === 'create'} noValidate>
                  <label>Ваше ім’я<input required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} placeholder="Наприклад, Софія" autoComplete="name" disabled={pendingAction !== null} /></label>
                  <label>Email<input required maxLength={320} type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="sofia@example.com" autoComplete="email" disabled={pendingAction !== null} /></label>
                  {createError !== null && <p className="form-error" role="alert">{createError}</p>}
                  <button className="btn primary full" disabled={pendingAction !== null}>{pendingAction === 'create' ? 'Створюємо…' : 'Створити профіль'} <Icon name="arrow" size={18} /></button>
                </form>
                <div className="divider"><span>Або увійти до наявного профілю</span></div>
                <form onSubmit={handleSelect} aria-busy={pendingAction === 'select'} noValidate>
                  <label>Email<input type="email" value={existingUserEmail} onChange={(event) => setExistingUserEmail(event.target.value)} placeholder="sofia@example.com" autoComplete="email" disabled={pendingAction !== null} /></label>
                  {selectError !== null && <p className="form-error" role="alert">{selectError}</p>}
                  <button className="btn secondary full" disabled={pendingAction !== null}>{pendingAction === 'select' ? 'Перевіряємо…' : 'Увійти за email'}</button>
                </form>
              </>
            )}
            <div className="profile-note"><Icon name="users" size={16} /><span>Ваш профіль — початок спільного кіновечора</span></div>
          </div>
        </section>
      </main>
      <PageFooter />
    </div>
  )
}
