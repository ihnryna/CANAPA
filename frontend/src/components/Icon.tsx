import type { ReactNode } from 'react'

interface IconProps {
  name: 'arrow' | 'back' | 'check' | 'close' | 'copy' | 'film' | 'link' | 'plus' | 'sofa' | 'users'
  size?: number
}

export function Icon({ name, size = 20 }: IconProps) {
  const paths: Record<IconProps['name'], ReactNode> = {
    arrow: <path d="m9 5 7 7-7 7M4 12h12" />,
    back: <path d="m14 5-7 7 7 7" />,
    check: <path d="m5 12 4 4L19 6" />,
    close: <path d="m6 6 12 12M18 6 6 18" />,
    copy: <><rect x="8" y="8" width="11" height="11" rx="2" /><path d="M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3" /></>,
    film: <><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M7 4v16M17 4v16M3 9h4m-4 6h4m10-6h4m-4 6h4" /></>,
    link: <><path d="m10 13 4-4m-6 6-2 2a4 4 0 0 1-6-6l4-4a4 4 0 0 1 6 0m4 2 2-2a4 4 0 0 1 6 6l-4 4a4 4 0 0 1-6 0" transform="translate(1 0)" /></>,
    plus: <path d="M12 5v14M5 12h14" />,
    sofa: <><path d="M5 12V7a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v5M5 19v2m14-2v2" /><path d="M5 11a2 2 0 0 0-4 0v7h22v-7a2 2 0 0 0-4 0v4H5Z" /></>,
    users: <><circle cx="9" cy="8" r="3" /><path d="M3 21v-3a6 6 0 0 1 12 0v3m1-16a3 3 0 0 1 0 6m2 4a5 5 0 0 1 3 5" /></>,
  }

  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name]}
    </svg>
  )
}
