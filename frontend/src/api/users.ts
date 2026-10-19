import { apiRequest } from './client'

export interface User {
  id: string
  name: string
  email: string
  createdAt: string
}

export interface CreateUserInput {
  name: string
  email: string
}

export function createUser(input: CreateUserInput): Promise<User> {
  return apiRequest<User>('/api/users', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function getUser(userId: string): Promise<User> {
  return apiRequest<User>(`/api/users/${encodeURIComponent(userId)}`)
}

export function getUserByEmail(email: string): Promise<User> {
  const query = new URLSearchParams({ email })
  return apiRequest<User>(`/api/users/by-email?${query.toString()}`)
}
