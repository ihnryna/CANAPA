import { apiRequest } from './client'

export interface UserPreference {
  id: number
  userId: string
  roomId: string
  likedGenres: string[]
  dislikedGenres: string[]
  minRating: number | null
  maxRuntime: number | null
  preferredLanguage: string | null
  textPreference: string | null
}

export interface SaveUserPreferenceRequest {
  userId: string
  likedGenres: string[]
  dislikedGenres: string[]
  minRating: number | null
  maxRuntime: number | null
  preferredLanguage: string | null
  textPreference: string | null
}

export function getRoomPreferences(
  roomId: string,
  signal?: AbortSignal,
): Promise<UserPreference[]> {
  return apiRequest<UserPreference[]>(
    `/api/rooms/${encodeURIComponent(roomId)}/preferences`,
    { signal },
  )
}

export function saveRoomPreference(
  roomId: string,
  preference: SaveUserPreferenceRequest,
): Promise<UserPreference> {
  return apiRequest<UserPreference>(
    `/api/rooms/${encodeURIComponent(roomId)}/preferences`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(preference),
    },
  )
}
