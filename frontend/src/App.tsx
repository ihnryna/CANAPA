import { Navigate, Route, Routes } from 'react-router-dom'
import './App.css'
import { UserSetupPage } from './pages/UserSetupPage'

function App() {
  return (
    <Routes>
      <Route path="/setup" element={<UserSetupPage />} />
      <Route path="*" element={<Navigate to="/setup" replace />} />
    </Routes>
  )
}

export default App
