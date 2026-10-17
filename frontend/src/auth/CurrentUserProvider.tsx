import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'
import { ApiClientError } from '../api/client'
import { getUser, type User } from '../api/users'
import {
  CurrentUserContext,
  type CurrentUserContextValue,
  type CurrentUserStatus,
} from './current-user-context'
import { isUuid } from './user-id'

const CURRENT_USER_STORAGE_KEY = 'canapa.currentUserId'

function readStoredUserId(): string | null {
  const storedUserId = window.localStorage.getItem(CURRENT_USER_STORAGE_KEY)
  if (storedUserId !== null && !isUuid(storedUserId)) {
    window.localStorage.removeItem(CURRENT_USER_STORAGE_KEY)
    return null
  }

  return storedUserId
}

export function CurrentUserProvider({ children }: PropsWithChildren) {
  const [storedUserId] = useState(readStoredUserId)
  const [user, setUser] = useState<User | null>(null)
  const [status, setStatus] = useState<CurrentUserStatus>(
    storedUserId === null ? 'ready' : 'loading',
  )
  const [restoreError, setRestoreError] = useState<string | null>(null)
  const [restoreAttempt, setRestoreAttempt] = useState(0)

  useEffect(() => {
    if (storedUserId === null) {
      return
    }

    let active = true

    getUser(storedUserId)
      .then((restoredUser) => {
        if (!active) return
        setUser(restoredUser)
        setStatus('ready')
      })
      .catch((error: unknown) => {
        if (!active) return

        if (error instanceof ApiClientError && error.status === 404) {
          window.localStorage.removeItem(CURRENT_USER_STORAGE_KEY)
          setUser(null)
          setStatus('ready')
          return
        }

        setRestoreError(
          error instanceof Error
            ? error.message
            : 'Could not restore the current user.',
        )
        setStatus('error')
      })

    return () => {
      active = false
    }
  }, [restoreAttempt, storedUserId])

  const selectUser = useCallback((selectedUser: User) => {
    window.localStorage.setItem(CURRENT_USER_STORAGE_KEY, selectedUser.id)
    setUser(selectedUser)
    setRestoreError(null)
    setStatus('ready')
  }, [])

  const clearUser = useCallback(() => {
    window.localStorage.removeItem(CURRENT_USER_STORAGE_KEY)
    setUser(null)
    setRestoreError(null)
    setStatus('ready')
  }, [])

  const retryRestore = useCallback(() => {
    setStatus('loading')
    setRestoreError(null)
    setRestoreAttempt((attempt) => attempt + 1)
  }, [])

  const value = useMemo<CurrentUserContextValue>(
    () => ({
      user,
      status,
      restoreError,
      selectUser,
      clearUser,
      retryRestore,
    }),
    [user, status, restoreError, selectUser, clearUser, retryRestore],
  )

  return (
    <CurrentUserContext.Provider value={value}>
      {children}
    </CurrentUserContext.Provider>
  )
}
