import { useCallback, useEffect, useRef, useState } from 'react'
import { getRoom, type Room } from '../api/rooms'
import { getRoomPreferences } from '../api/preferences'

interface RoomPollingState {
  room: Room | null
  readyUserIds: Set<string> | null
  loading: boolean
  error: string | null
  preferencesError: boolean
  lastUpdatedAt: Date | null
  refresh: () => Promise<void>
}

export function useRoomPolling(roomId: string | undefined): RoomPollingState {
  const [room, setRoom] = useState<Room | null>(null)
  const [readyUserIds, setReadyUserIds] = useState<Set<string> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [preferencesError, setPreferencesError] = useState(false)
  const [lastUpdatedAt, setLastUpdatedAt] = useState<Date | null>(null)
  const controllerRef = useRef<AbortController | null>(null)

  const refresh = useCallback(async () => {
    if (roomId === undefined) return
    controllerRef.current?.abort()
    const controller = new AbortController()
    controllerRef.current = controller
    try {
      const loadedRoom = await getRoom(roomId, controller.signal)
      if (controller.signal.aborted) return
      setRoom(loadedRoom)
      setError(null)
      setLastUpdatedAt(new Date())
      try {
        const preferences = await getRoomPreferences(roomId, controller.signal)
        if (controller.signal.aborted) return
        setReadyUserIds(new Set(preferences.map((preference) => preference.userId)))
        setPreferencesError(false)
      } catch {
        if (controller.signal.aborted) return
        setReadyUserIds(null)
        setPreferencesError(true)
      }
    } catch (caught) {
      if (controller.signal.aborted) return
      setError(caught instanceof Error ? caught.message : 'Не вдалося завантажити кімнату.')
    } finally {
      if (!controller.signal.aborted) setLoading(false)
    }
  }, [roomId])

  useEffect(() => {
    let timer: number | undefined
    let active = true

    const schedule = async () => {
      await refresh()
      if (active) timer = window.setTimeout(schedule, 7000)
    }
    void schedule()

    const handleVisibility = () => {
      if (document.visibilityState === 'visible') void refresh()
    }
    document.addEventListener('visibilitychange', handleVisibility)
    return () => {
      active = false
      if (timer !== undefined) window.clearTimeout(timer)
      controllerRef.current?.abort()
      document.removeEventListener('visibilitychange', handleVisibility)
    }
  }, [refresh])

  return { room, readyUserIds, loading, error, preferencesError, lastUpdatedAt, refresh }
}
