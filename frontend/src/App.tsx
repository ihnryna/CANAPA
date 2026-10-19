import { Navigate, Route, Routes } from 'react-router-dom'
import './App.css'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { CreateRoomPage } from './pages/CreateRoomPage'
import { JoinRoomPage } from './pages/JoinRoomPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { RoomPage } from './pages/RoomPage'
import { RoomsPage } from './pages/RoomsPage'
import { UserSetupPage } from './pages/UserSetupPage'

function App() {
  return (
    <Routes>
      <Route path="/" element={<UserSetupPage />} />
      <Route path="/setup" element={<UserSetupPage />} />
      <Route path="/join/:code" element={<JoinRoomPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/rooms" element={<RoomsPage />} />
        <Route path="/rooms/new" element={<CreateRoomPage />} />
        <Route path="/rooms/:roomId" element={<RoomPage />} />
      </Route>
      <Route path="/home" element={<Navigate to="/rooms" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default App
