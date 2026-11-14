import { useEffect, useMemo, useState } from 'react'

export type Theme = 'light' | 'dark'

const STORAGE_KEY = 'itsm.theme'
const CHANGE_EVENT = 'itsm:theme'

function systemTheme(): Theme {
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/** The theme in effect: the user's saved choice (set on <html data-theme>) or the system preference. */
export function currentTheme(): Theme {
  const chosen = document.documentElement.getAttribute('data-theme')
  return chosen === 'light' || chosen === 'dark' ? chosen : systemTheme()
}

export function setTheme(theme: Theme) {
  document.documentElement.setAttribute('data-theme', theme)
  try {
    localStorage.setItem(STORAGE_KEY, theme)
  } catch {
    // Storage may be blocked; the choice still applies for this visit.
  }
  window.dispatchEvent(new Event(CHANGE_EVENT))
}

/** Current theme plus a toggle; every component using it re-renders when the theme changes. */
export function useTheme() {
  const [theme, setThemeState] = useState<Theme>(currentTheme)

  useEffect(() => {
    const sync = () => setThemeState(currentTheme())
    const media = window.matchMedia?.('(prefers-color-scheme: dark)')
    window.addEventListener(CHANGE_EVENT, sync)
    media?.addEventListener('change', sync)
    return () => {
      window.removeEventListener(CHANGE_EVENT, sync)
      media?.removeEventListener('change', sync)
    }
  }, [])

  return {
    theme,
    toggle: () => setTheme(theme === 'dark' ? 'light' : 'dark'),
  }
}

function token(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

export interface ChartColors {
  created: string
  resolved: string
  grid: string
  axis: string
  tooltip: { background: string; border: string; color: string }
  priority: Record<'P1' | 'P2' | 'P3' | 'P4', string>
}

/** SVG charts cannot read CSS variables, so this resolves the palette tokens for the current theme. */
export function useChartColors(): ChartColors {
  const { theme } = useTheme()
  return useMemo(
    () => ({
      created: token('--accent'),
      // Gold, not green: it must differ from the teal "created" series in hue and lightness.
      resolved: token('--star'),
      grid: token('--line'),
      axis: token('--muted'),
      tooltip: { background: token('--surface-raised'), border: token('--line'), color: token('--ink') },
      priority: {
        P1: token('--danger'),
        P2: token('--warning'),
        P3: token('--info'),
        P4: token('--muted'),
      },
    }),
    // Recompute whenever the theme flips.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [theme],
  )
}
