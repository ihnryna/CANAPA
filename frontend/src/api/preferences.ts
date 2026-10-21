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
  likedGenres: string[]
  dislikedGenres: string[]
  minRating: number | null
  maxRuntime: number | null
  preferredLanguage: string | null
  textPreference: string | null
}

function preferencesUrl(roomId: string): string {
  return `/api/rooms/${encodeURIComponent(roomId)}/preferences`
}

export function getRoomPreferences(
  roomId: string,
  signal?: AbortSignal,
): Promise<UserPreference[]> {
  return apiRequest<UserPreference[]>(
    preferencesUrl(roomId),
    { signal },
  )
}

export function getUserPreference(
  roomId: string,
  userId: string,
  signal?: AbortSignal,
): Promise<UserPreference> {
  return apiRequest<UserPreference>(
    `${preferencesUrl(roomId)}/users/${encodeURIComponent(userId)}`,
    { signal },
  )
}

export function saveRoomPreference(
  roomId: string,
  userId: string,
  preference: SaveUserPreferenceRequest,
): Promise<UserPreference> {
  return apiRequest<UserPreference>(
    `${preferencesUrl(roomId)}/users/${encodeURIComponent(userId)}`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(preference),
    },
  )
}

export function getGroupPreferenceSummary(
  roomId: string,
  signal?: AbortSignal,
): Promise<unknown> {
  return apiRequest<unknown>(
    `${preferencesUrl(roomId)}/summary`,
    { signal },
  )
}

export function deleteUserPreference(
  roomId: string,
  userId: string,
): Promise<void> {
  return apiRequest<void>(
    `${preferencesUrl(roomId)}/users/${encodeURIComponent(userId)}`,
    { method: 'DELETE' },
  )
}
