import { Navigate, Route, Routes } from 'react-router-dom'
import './App.css'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { CreateRoomPage } from './pages/CreateRoomPage'
import { JoinRoomPage } from './pages/JoinRoomPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { RoomPage } from './pages/RoomPage'
import { RecommendationPage } from './pages/RecommendationPage'
import { RoomsPage } from './pages/RoomsPage'
import { UserSetupPage } from './pages/UserSetupPage'
import { VotingPage } from './pages/VotingPage'
import { MovieDiscoveryPage } from "./pages/MovieDiscoveryPage.tsx";

function App() {
  return (
    <Routes>
      <Route path="/" element={<UserSetupPage/>}/>
      <Route path="/setup" element={<UserSetupPage/>}/>
      <Route path="/join/:code" element={<JoinRoomPage/>}/>

      <Route element={<ProtectedRoute/>}>
        <Route path="/rooms" element={<RoomsPage/>}/>
        <Route path="/rooms/new" element={<CreateRoomPage/>}/>
        <Route path="/rooms/:roomId" element={<RoomPage/>}/>
        <Route path="/rooms/:roomId/recommendations" element={<RecommendationPage/>}/>
        <Route path="/rooms/:roomId/voting" element={<VotingPage/>}/>
        <Route path="/rooms/:roomId/discover" element={<MovieDiscoveryPageRoute/>}/>
      </Route>

      <Route path="/home" element={<Navigate to="/rooms" replace/>}/>
      <Route path="*" element={<NotFoundPage/>}/>
    </Routes>
  )
}

function MovieDiscoveryPageRoute() {
  const userId = localStorage.getItem('canapa.currentUserId') ?? ''
  return <MovieDiscoveryPage userId={userId}/>
}

export default App
