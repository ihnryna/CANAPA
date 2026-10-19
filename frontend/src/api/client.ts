export interface ApiErrorResponse {
  code: string
  message: string
  status: number
  path: string
  timestamp: string
}

interface ProblemDetailResponse {
  detail?: string
  status: number
  title?: string
}

interface SpringErrorResponse {
  error?: string
  message?: string
  status: number
}

export class ApiClientError extends Error {
  readonly code: string
  readonly status: number | null

  constructor(message: string, code: string, status: number | null) {
    super(message)
    this.name = 'ApiClientError'
    this.code = code
    this.status = status
  }
}

const configuredBaseUrl =
  import.meta.env.VITE_API_BASE_URL?.trim() || ''

export const API_BASE_URL = configuredBaseUrl.replace(/\/+$/, '')

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const candidate = value as Record<string, unknown>
  return (
    typeof candidate.code === 'string' &&
    typeof candidate.message === 'string' &&
    typeof candidate.status === 'number'
  )
}

function hasNumericStatus(value: unknown): value is Record<string, unknown> & {
  status: number
} {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as Record<string, unknown>).status === 'number'
  )
}

function isProblemDetailResponse(value: unknown): value is ProblemDetailResponse {
  return (
    hasNumericStatus(value) &&
    (typeof value.detail === 'string' || typeof value.title === 'string')
  )
}

function isSpringErrorResponse(value: unknown): value is SpringErrorResponse {
  return (
    hasNumericStatus(value) &&
    (typeof value.message === 'string' || typeof value.error === 'string')
  )
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')

  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers })
  } catch {
    throw new ApiClientError(
      'Could not reach the CANAPA server. Check that the backend is running.',
      'NETWORK_ERROR',
      null,
    )
  }

  if (!response.ok) {
    let body: unknown
    try {
      body = await response.json()
    } catch {
      throw new ApiClientError(
        'The server returned an unexpected error response.',
        'UNEXPECTED_RESPONSE',
        response.status,
      )
    }

    if (isApiErrorResponse(body)) {
      throw new ApiClientError(body.message, body.code, body.status)
    }

    if (isProblemDetailResponse(body)) {
      throw new ApiClientError(
        body.detail || body.title || 'The server rejected the request.',
        'PROBLEM_DETAIL',
        body.status,
      )
    }

    if (isSpringErrorResponse(body)) {
      throw new ApiClientError(
        body.message || body.error || 'The server rejected the request.',
        'SERVER_ERROR',
        body.status,
      )
    }

    throw new ApiClientError(
      'The server returned an unexpected error response.',
      'UNEXPECTED_RESPONSE',
      response.status,
    )
  }

  if (response.status === 204) {
    return undefined as T
  }

  try {
    return (await response.json()) as T
  } catch {
    throw new ApiClientError(
      'The server returned an invalid success response.',
      'UNEXPECTED_RESPONSE',
      response.status,
    )
  }
}
