import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type Theme = 'light' | 'dark'

interface ThemeStoreState {
  theme: Theme
  setTheme: (theme: Theme) => void
  toggleTheme: () => void
}

function prefersDark(): boolean {
  if (typeof window === 'undefined') return false
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

function initialTheme(): Theme {
  const stored = typeof localStorage !== 'undefined' ? localStorage.getItem('driveai-theme') : null
  if (stored === '"light"' || stored === '"dark"') return stored.slice(1, -1) as Theme
  return prefersDark() ? 'dark' : 'light'
}

export function applyTheme(theme: Theme): void {
  const root = document.documentElement
  if (theme === 'dark') {
    root.classList.add('dark')
  } else {
    root.classList.remove('dark')
  }
  root.style.colorScheme = theme
}

export const useThemeStore = create<ThemeStoreState>()(
  persist(
    (set) => ({
      theme: initialTheme(),
      setTheme: (theme) => {
        applyTheme(theme)
        set({ theme })
      },
      toggleTheme: () =>
        set((state) => {
          const next: Theme = state.theme === 'dark' ? 'light' : 'dark'
          applyTheme(next)
          return { theme: next }
        }),
    }),
    { name: 'driveai-theme' },
  ),
)

applyTheme(useThemeStore.getState().theme)

export const selectTheme = (state: ThemeStoreState): Theme => state.theme
export const selectToggleTheme = (state: ThemeStoreState): () => void => state.toggleTheme