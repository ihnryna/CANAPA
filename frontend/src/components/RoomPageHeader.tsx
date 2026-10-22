import { Link } from "react-router-dom";
import { Icon } from "./Icon.tsx";

import type { NavigateFunction } from "react-router-dom";
import type { Room } from "../api/rooms.ts";

interface RoomPageHeaderProps {
  error: string | null;
  refresh: () => Promise<void>;
  member: boolean;
  room: Room;
  STATUS_LABELS: {
    readonly CREATED: string;
    readonly VOTING: string;
    readonly FINISHED: string;
  };
  copyRoomId: () => Promise<void>;
  lastUpdatedAt: Date | null;
  navigate: NavigateFunction;
  currentIdx: number;
}

export function RoomPageHeader(
  {
    error,
    refresh,
    member,
    room,
    STATUS_LABELS,
    copyRoomId,
    lastUpdatedAt,
    navigate,
    currentIdx
  }: RoomPageHeaderProps
) {
  return (
    <>
      <Link className="back-link" to="/rooms"><Icon name="back" size={16}/> Мої кімнати</Link>
      {error !== null && <div className="polling-alert" role="alert"><span>Оновлення призупинено: {error}</span>
          <button className="text-button" onClick={() => void refresh()}>Повторити</button>
      </div>}
      {!member && <div className="polling-alert warning" role="alert">Ваш профіль не входить до списку учасників. Дані
          доступні через backend, але дії кімнати приховано.</div>}

      <section className="room-top">
        <div>
          <div className="eyebrow">ВАША КІНОКОМПАНІЯ</div>
          <h1>{room.name}</h1></div>
        <span className={`status-badge status-${room.status.toLowerCase()}`}>{STATUS_LABELS[room.status]}</span>
      </section>
      <div className="room-meta"><span>ID: <code>{room.id}</code></span>
        <button className="text-button compact" onClick={copyRoomId}><Icon name="copy" size={15}/> Копіювати ID
        </button>
        <span className="polling-note">{lastUpdatedAt === null ? 'Оновлюємо…' : 'Автоматично оновлюється'}</span>
      </div>
      {room.status === 'VOTING' && (
        <div className="voting-entry">
          <Link className="btn primary" to={`/rooms/${room.id}/voting`}>
            Перейти до голосування
            <Icon name="arrow" size={17}/>
          </Link>
        </div>
      )}
      <nav className="room-steps" aria-label="Етапи кімнати">
        {[
          'Учасники',
          'Уподобання',
          'Голосування',
          'Результат',
        ].map((step, index) => (
          <button
            key={step}
            type="button"
            className={index === currentIdx ? 'active' : ''}
            disabled={!member}
            onClick={() => {
              if (index === 0) {
                navigate(`/rooms/${room.id}`)
              }
              if (index === 1) {
                navigate(`/rooms/${room.id}/discover`)
              }
              if (index === 2) {
                navigate(`/rooms/${room.id}/voting`)
              }
            }}
          >
            <span>0{index + 1}</span>
            {step}
          </button>
        ))}
      </nav>
    </>
  )
}