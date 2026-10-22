import { apiRequest } from './client'

export type RoomStatus = 'CREATED' | 'VOTING' | 'FINISHED'

export interface RoomMember {
  id: string
  name: string
}

export interface Room {
  id: string
  name: string
  creator: RoomMember
  participants: RoomMember[]
  status: RoomStatus
  createdAt: string
}

function userHeaders(userId: string): HeadersInit {
  return { 'X-User-Id': userId }
}

export function createRoom(name: string, userId: string): Promise<Room> {
  return apiRequest<Room>('/api/rooms', {
    method: 'POST',
    headers: userHeaders(userId),
    body: JSON.stringify({ name }),
  })
}

export function getRoom(roomId: string, signal?: AbortSignal): Promise<Room> {
  return apiRequest<Room>(`/api/rooms/${encodeURIComponent(roomId)}`, { signal })
}

export function getRooms(userId: string, signal?: AbortSignal): Promise<Room[]> {
  return apiRequest<Room[]>('/api/rooms', {
    headers: userHeaders(userId),
    signal,
  })
}

export function joinRoom(roomId: string, userId: string): Promise<Room> {
  return apiRequest<Room>(
    `/api/rooms/${encodeURIComponent(roomId)}/participants`,
    {
      method: 'POST',
      headers: userHeaders(userId),
    },
  )
}

export function changeRoomStatus(
  roomId: string,
  userId: string,
  status: RoomStatus,
): Promise<Room> {
  return apiRequest<Room>(
    `/api/rooms/${encodeURIComponent(roomId)}/status`,
    {
      method: 'PUT',
      headers: userHeaders(userId),
      body: JSON.stringify({ status }),
    },
  )
}
