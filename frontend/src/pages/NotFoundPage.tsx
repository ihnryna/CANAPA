import { Link } from 'react-router-dom'
import { AppHeader } from '../components/AppHeader'
import { Icon } from '../components/Icon'

export function NotFoundPage() {
  return (
    <div className="app-shell"><AppHeader />
      <main className="centered-state">
        <span className="state-symbol">404</span>
        <h1>Такої сторінки немає</h1>
        <p>Перевірте адресу або поверніться до кімнат.</p>
        <Link className="btn primary" to="/rooms">До моїх кімнат <Icon name="arrow" size={17} /></Link>
      </main>
    </div>
  )
}
