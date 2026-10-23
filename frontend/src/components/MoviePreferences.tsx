import { useCallback, useEffect, useState } from 'react'
import {
  getRoomPreferences,
  saveRoomPreference,
  type UserPreference,
} from '../api/preferences'
import './MoviePreferences.css'
import { useNavigate } from "react-router-dom";
import { useRoomPolling } from "../hooks/useRoomPolling.ts";
import { isUuid } from "../auth/user-id.ts";

interface MoviePreferencesProps {
  roomId: string
  userId: string
  onSaved?: (preference: UserPreference) => void
}

const GENRES = [
  'Action',
  'Adventure',
  'Animation',
  'Comedy',
  'Crime',
  'Documentary',
  'Drama',
  'Family',
  'Fantasy',
  'History',
  'Horror',
  'Music',
  'Mystery',
  'Romance',
  'Science Fiction',
  'Thriller',
  'War',
  'Western',
]

const LANGUAGES = [
  { value: 'uk', label: 'Українська' },
  { value: 'en', label: 'English' },
  { value: 'fr', label: 'Français' },
  { value: 'de', label: 'Deutsch' },
  { value: 'es', label: 'Español' },
  { value: 'ja', label: '日本語' },
  { value: 'ko', label: '한국어' },
]

interface PreferenceForm {
  likedGenres: string[]
  dislikedGenres: string[]
  minRating: number
  maxRuntime: number
  preferredLanguage: string
  textPreference: string
}

const DEFAULT_FORM: PreferenceForm = {
  likedGenres: [],
  dislikedGenres: [],
  minRating: 6,
  maxRuntime: 180,
  preferredLanguage: 'uk',
  textPreference: '',
}

function toForm(preference?: UserPreference): PreferenceForm {
  if (!preference) return { ...DEFAULT_FORM }

  return {
    likedGenres: preference.likedGenres ?? [],
    dislikedGenres: preference.dislikedGenres ?? [],
    minRating: preference.minRating ?? 6,
    maxRuntime: preference.maxRuntime ?? 180,
    preferredLanguage: preference.preferredLanguage ?? 'uk',
    textPreference: preference.textPreference ?? '',
  }
}

export function MoviePreferences(
  {
    roomId,
    userId,
    onSaved,
  }: MoviePreferencesProps) {
  const [form, setForm] = useState<PreferenceForm>(DEFAULT_FORM)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const navigate = useNavigate()
  const validRoomId =
    roomId !== undefined && isUuid(roomId) ? roomId : undefined
  const { room } = useRoomPolling(validRoomId)

  useEffect(() => {
    const controller = new AbortController()

    async function loadPreferences() {
      setLoading(true)
      setError('')

      try {
        const preferences = await getRoomPreferences(
          roomId,
          controller.signal,
        )

        const mine = preferences.find(
          (preference) => String(preference.userId) === String(userId),
        )

        setForm(toForm(mine))
      } catch {
        if (!controller.signal.aborted) {
          setError('Не вдалося завантажити вподобання. Спробуйте ще раз.')
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }

    if (roomId && userId) {
      void loadPreferences()
    } else {
      setLoading(false)
      setError('Не вдалося визначити кімнату або користувача.')
    }

    return () => controller.abort()
  }, [roomId, userId])

  const toggleGenre = useCallback(
    (genre: string, type: 'likedGenres' | 'dislikedGenres') => {
      setForm((current) => {
        const selected = current[type]
        const next = selected.includes(genre)
          ? selected.filter((item) => item !== genre)
          : [...selected, genre]

        const otherType =
          type === 'likedGenres' ? 'dislikedGenres' : 'likedGenres'

        return {
          ...current,
          [type]: next,
          [otherType]: current[otherType].filter((item) => item !== genre),
        }
      })

      setSuccess(false)
    },
    [],
  )

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSuccess(false)

    if (!roomId || !userId) {
      setError('Не вдалося визначити кімнату або користувача.')
      return
    }

    setSaving(true)

    try {
      const saved = await saveRoomPreference(roomId, userId, {
        likedGenres: form.likedGenres,
        dislikedGenres: form.dislikedGenres,
        minRating: form.minRating,
        maxRuntime: form.maxRuntime,
        preferredLanguage: form.preferredLanguage,
        textPreference: form.textPreference.trim() || null,
      })

      setSuccess(true)
      onSaved?.(saved)
    } catch {
      setError('Не вдалося зберегти вподобання. Спробуйте ще раз.')
    } finally {
      setSaving(false)
    }
  }

  const handleSubmitAndNavigate = async (
    event: React.FormEvent<HTMLFormElement>
  ) => {
    await handleSubmit(event)
    if (!error && room) {
      navigate(`/rooms/${room.id}`)
    }
  }

  if (loading) {
    return (
      <section className="preferences-panel">
        <div className="preferences-loading">
          <span className="spinner"/>
          <p>Завантажуємо ваші вподобання...</p>
        </div>
      </section>
    )
  }

  return (
    <section className="preferences-panel">
      <div className="preferences-header">
        <span className="eyebrow">YOUR MOVIE TASTE</span>
        <h2>Ваші кіновподобання</h2>
        <p>
          Оберіть жанри, рейтинг і тривалість. Ми використаємо ці
          налаштування для підбору фільмів для вашої компанії.
        </p>
      </div>

      <form onSubmit={handleSubmitAndNavigate}>
        <fieldset className="preference-fieldset">
          <legend>Які жанри вам подобаються?</legend>
          <p className="preference-hint">
            Можна обрати декілька варіантів.
          </p>

          <div className="genre-options">
            {GENRES.map((genre) => (
              <button
                key={genre}
                type="button"
                className={`genre-chip ${
                  form.likedGenres.includes(genre) ? 'selected' : ''
                }`}
                aria-pressed={form.likedGenres.includes(genre)}
                onClick={() => toggleGenre(genre, 'likedGenres')}
              >
                {form.likedGenres.includes(genre) && (
                  <span aria-hidden="true">✓ </span>
                )}
                {genre}
              </button>
            ))}
          </div>
        </fieldset>

        <fieldset className="preference-fieldset">
          <legend>Яких жанрів бажаєте уникати?</legend>
          <div className="genre-options">
            {GENRES.map((genre) => (
              <button
                key={genre}
                type="button"
                className={`genre-chip genre-chip-disliked ${
                  form.dislikedGenres.includes(genre) ? 'selected' : ''
                }`}
                aria-pressed={form.dislikedGenres.includes(genre)}
                onClick={() => toggleGenre(genre, 'dislikedGenres')}
              >
                {form.dislikedGenres.includes(genre) && (
                  <span aria-hidden="true">× </span>
                )}
                {genre}
              </button>
            ))}
          </div>
        </fieldset>

        <div className="preference-fields-grid">
          <label className="preference-label">
            Мінімальний рейтинг
            <span className="preference-input-with-suffix">
              <input
                type="number"
                min="0"
                max="10"
                step="0.5"
                value={form.minRating}
                onChange={(event) => {
                  setForm((current) => ({
                    ...current,
                    minRating: Number(event.target.value),
                  }))
                  setSuccess(false)
                }}
              />
              <span>/ 10</span>
            </span>
          </label>

          <label className="preference-label">
            Максимальна тривалість
            <span className="preference-input-with-suffix">
              <input
                type="number"
                min="30"
                max="360"
                step="15"
                value={form.maxRuntime}
                onChange={(event) => {
                  setForm((current) => ({
                    ...current,
                    maxRuntime: Number(event.target.value),
                  }))
                  setSuccess(false)
                }}
              />
              <span>хв</span>
            </span>
          </label>

          <label className="preference-label">
            Мова фільмів
            <select
              value={form.preferredLanguage}
              onChange={(event) => {
                setForm((current) => ({
                  ...current,
                  preferredLanguage: event.target.value,
                }))
                setSuccess(false)
              }}
            >
              {LANGUAGES.map((language) => (
                <option key={language.value} value={language.value}>
                  {language.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}

        {success && (
          <p className="preferences-success" role="status">
            ✓ Ваші вподобання успішно збережено.
          </p>
        )}

        <div className="preferences-actions">
          <button
            className="btn secondary"
            type="button"
            onClick={() => {
              setForm({ ...DEFAULT_FORM })
              setSuccess(false)
            }}
            disabled={saving}
          >
            Скинути
          </button>
          <button className="btn primary" type="submit" disabled={saving}>
            {saving ? 'Зберігаємо...' : 'Зберегти вподобання'}
          </button>
        </div>
      </form>
    </section>
  )
}
