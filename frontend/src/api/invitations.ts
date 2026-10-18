import { apiRequest } from './client'

export type InvitationStatus = 'ACTIVE' | 'EXPIRED'

export interface Invitation {
  id: string
  roomId: string
  createdByUserId: string
  code: string
  link: string
  createdAt: string
  expiresAt: string
  status: InvitationStatus
}

function userHeaders(userId: string): HeadersInit {
  return { 'X-User-Id': userId }
}

export function createInvitation(
  roomId: string,
  userId: string,
  expiresInHours: number,
): Promise<Invitation> {
  return apiRequest<Invitation>(
    `/api/rooms/${encodeURIComponent(roomId)}/invitations`,
    {
      method: 'POST',
      headers: userHeaders(userId),
      body: JSON.stringify({ expiresInHours }),
    },
  )
}

export function validateInvitation(
  code: string,
  userId: string,
  signal?: AbortSignal,
): Promise<Invitation> {
  return apiRequest<Invitation>(
    `/api/invitations/${encodeURIComponent(code)}/join`,
    {
      method: 'POST',
      headers: userHeaders(userId),
      signal,
    },
  )
}
