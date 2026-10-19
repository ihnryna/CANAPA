import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useCurrentUser } from './current-user-context'

export function ProtectedRoute() {
  const location = useLocation()
  const { user, status, restoreError, retryRestore, clearUser } = useCurrentUser()

  if (status === 'loading') {
    return (
      <main className="centered-state" aria-busy="true">
        <span className="spinner" aria-hidden="true" />
        <h1>Відновлюємо профіль</h1>
        <p>Ще мить — готуємо ваш простір для кіно.</p>
      </main>
    )
  }

  if (status === 'error') {
    return (
      <main className="centered-state">
        <span className="state-symbol" aria-hidden="true">!</span>
        <h1>Не вдалося відновити профіль</h1>
        <p role="alert">{restoreError}</p>
        <div className="actions">
          <button className="btn primary" onClick={retryRestore}>Спробувати ще раз</button>
          <button className="btn secondary" onClick={clearUser}>Змінити користувача</button>
        </div>
      </main>
    )
  }

  if (user === null) {
    const returnTo = `${location.pathname}${location.search}`
    return <Navigate to={`/?returnTo=${encodeURIComponent(returnTo)}`} replace />
  }

  return <Outlet />
}
