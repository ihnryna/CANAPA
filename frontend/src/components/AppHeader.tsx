import { Link } from 'react-router-dom'
import { useCurrentUser } from '../auth/current-user-context'
import { Icon } from './Icon'

export function AppHeader() {
  const { user } = useCurrentUser()

  return (
    <header className="site-header">
      <div className="header-inner">
        <Link className="brand" to="/setup" aria-label="CANAPA — налаштування профілю">
          <span className="brand-mark"><Icon name="sofa" size={25} /></span>
          CANAPA<span className="brand-dot">.</span>
        </Link>
        <div className="header-right">
          <span className="header-caption">Кіно об’єднує</span>
          {user === null ? (
            <span className="header-icon"><Icon name="film" /></span>
          ) : (
            <Link className="avatar" to="/setup" aria-label={`Профіль ${user.name}`}>
              {user.name.trim().charAt(0).toUpperCase()}
            </Link>
          )}
        </div>
      </div>
    </header>
  )
}
