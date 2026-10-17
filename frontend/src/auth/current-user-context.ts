import { createContext, useContext } from 'react'
import type { User } from '../api/users'

export type CurrentUserStatus = 'loading' | 'ready' | 'error'

export interface CurrentUserContextValue {
  user: User | null
  status: CurrentUserStatus
  restoreError: string | null
  selectUser: (user: User) => void
  clearUser: () => void
  retryRestore: () => void
}

export const CurrentUserContext =
  createContext<CurrentUserContextValue | null>(null)

export function useCurrentUser(): CurrentUserContextValue {
  const context = useContext(CurrentUserContext)

  if (context === null) {
    throw new Error('useCurrentUser must be used inside CurrentUserProvider')
  }

  return context
}
