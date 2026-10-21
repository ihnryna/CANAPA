import { apiRequest } from './client'
import type { Movie } from './movies'

export type RecommendationType =
  | 'CONSENSUS'
  | 'AVERAGE_RATING'
  | 'GENRE_FOCUSED'

export interface RoomRecommendedMovie {
  movie: Movie
  score: number
}

export function getRoomRecommendations(
  roomId: string,
  userId: string,
  strategy: RecommendationType = 'CONSENSUS',
  limit = 40,
  signal?: AbortSignal,
): Promise<RoomRecommendedMovie[]> {
  const params = new URLSearchParams({
    strategy,
    limit: String(limit),
  })

  return apiRequest<RoomRecommendedMovie[]>(
    `/api/rooms/${encodeURIComponent(roomId)}/recommendations?${params}`,
    {
      headers: { 'X-User-Id': userId },
      signal,
    },
  )
}
