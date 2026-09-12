import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import {
  login as apiLogin,
  register as apiRegister,
  setAuthToken,
} from '../services/api'
import type { AuthResponse, User } from '../types'

interface AuthStoreState {
  token: string | null
  user: User | null
  login: (email: string, password: string) => Promise<AuthResponse>
  register: (email: string, password: string, fullName: string) => Promise<AuthResponse>
  logout: () => void
}

export const useAuthStore = create<AuthStoreState>()(
  persist(
    (set) => ({
      token: null,
      user: null,

      login: async (email, password) => {
        const response = await apiLogin({ email, password })
        setAuthToken(response.token)
        set({ token: response.token, user: response.user })
        return response
      },

      register: async (email, password, fullName) => {
        const response = await apiRegister({ email, password, fullName })
        setAuthToken(response.token)
        set({ token: response.token, user: response.user })
        return response
      },

      logout: () => {
        setAuthToken(null)
        set({ token: null, user: null })
      },
    }),
    { name: 'driveai-auth' },
  ),
)

const rehydrated = useAuthStore.getState()
setAuthToken(rehydrated.token)

if (typeof window !== 'undefined') {
  window.addEventListener('driveai:unauthorized', () => {
    useAuthStore.getState().logout()
  })
}

export const selectUser = (state: AuthStoreState): User | null => state.user
export const selectAuthenticated = (state: AuthStoreState): boolean =>
  state.token !== null && state.user !== null
export const selectIsAdmin = (state: AuthStoreState): boolean =>
  state.user?.role === 'ADMIN'
export const selectLogout = (state: AuthStoreState): () => void => state.logout