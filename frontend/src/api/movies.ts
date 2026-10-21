import { apiRequest } from './client'

export interface Movie {
  id: number
  tmdbId: number | null
  imdbId: string | null
  title: string
  description: string | null
  genres: string[]
  rating: number | null
  runtime: number | null
  releaseYear: number | null
  posterUrl: string | null
  originalLanguage: string | null
  director: string | null
  actors: string | null
  writers: string | null
  rated: string | null
}

export interface MovieSearchResponse {
  page: number
  totalPages: number
  totalResults: number
  results: Movie[]
}

export function getPopularMovies(
  limit = 40,
  signal?: AbortSignal,
): Promise<Movie[]> {
  const query = new URLSearchParams({ limit: String(limit) })

  return apiRequest<Movie[]>(
    `/api/movies/popular?${query.toString()}`,
    { signal },
  )
}

export function searchMovies(
  query: string,
  page = 1,
  size = 40,
  language = 'uk-UA',
  signal?: AbortSignal,
): Promise<MovieSearchResponse> {
  const params = new URLSearchParams({
    query,
    page: String(page),
    size: String(size),
    language,
  })

  return apiRequest<MovieSearchResponse>(
    `/api/movies/search?${params.toString()}`,
    { signal },
  )
}
