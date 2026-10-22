import { apiRequest } from './client'
import type { Movie } from './movies'

export interface Vote {
  id: number
  userId: string
  roomId: string
  movieId: number
  createdAt: string
}

export interface ExcludedMovie {
  id: number
  roomId: string
  movieId: number
  excludedByUserId: string
  excludedAt: string
}

export interface VotingResult {
  id: number
  roomId: string
  winnerMovieIds: number[]
  winners: Movie[]
  voteCount: number
  finishedAt: string
}

export function getRoomVotes(roomId: string): Promise<Vote[]> {
  return apiRequest<Vote[]>(
    `/api/rooms/${encodeURIComponent(roomId)}/votes`,
  )
}

export function getUserVotes(
  roomId: string,
  userId: string,
): Promise<Vote[]> {
  const query = new URLSearchParams({ userId })

  return apiRequest<Vote[]>(
    `/api/rooms/${encodeURIComponent(roomId)}/votes/me?${query.toString()}`,
  )
}

export function addVote(
  roomId: string,
  userId: string,
  movieId: number,
): Promise<Vote> {
  return apiRequest<Vote>(
    `/api/rooms/${encodeURIComponent(roomId)}/votes`,
    {
      method: 'POST',
      body: JSON.stringify({ userId, movieId }),
    },
  )
}

export function removeVote(
  roomId: string,
  movieId: number,
  userId: string,
): Promise<void> {
  const query = new URLSearchParams({ userId })

  return apiRequest<void>(
    `/api/rooms/${encodeURIComponent(roomId)}/votes/${encodeURIComponent(String(movieId))}?${query.toString()}`,
    { method: 'DELETE' },
  )
}

export function getExcludedMovies(
  roomId: string,
): Promise<ExcludedMovie[]> {
  return apiRequest<ExcludedMovie[]>(
    `/api/rooms/${encodeURIComponent(roomId)}/excluded-movies`,
  )
}

export function excludeMovie(
  roomId: string,
  userId: string,
  movieId: number,
): Promise<ExcludedMovie> {
  return apiRequest<ExcludedMovie>(
    `/api/rooms/${encodeURIComponent(roomId)}/excluded-movies`,
    {
      method: 'POST',
      body: JSON.stringify({ userId, movieId }),
    },
  )
}

export function restoreMovie(
  roomId: string,
  movieId: number,
  userId: string,
): Promise<void> {
  const query = new URLSearchParams({ userId })

  return apiRequest<void>(
    `/api/rooms/${encodeURIComponent(roomId)}/excluded-movies/${encodeURIComponent(String(movieId))}?${query.toString()}`,
    { method: 'DELETE' },
  )
}

export function finishVoting(roomId: string): Promise<VotingResult> {
  return apiRequest<VotingResult>(
    `/api/rooms/${encodeURIComponent(roomId)}/voting-result`,
    { method: 'POST' },
  )
}

export function getVotingResult(
  roomId: string,
  signal?: AbortSignal,
): Promise<VotingResult> {
  return apiRequest<VotingResult>(
    `/api/rooms/${encodeURIComponent(roomId)}/voting-result`,
    { signal },
  )
}
