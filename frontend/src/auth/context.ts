import { createContext, useContext } from 'react'

import type { AuthResponse, User } from '../lib/types'

export interface AuthState {
  user: User | null
  ready: boolean
  signIn: (auth: AuthResponse) => void
  signOut: () => void
  refresh: () => Promise<void>
}

export const AuthContext = createContext<AuthState | null>(null)

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth doit être utilisé dans AuthProvider')
  return ctx
}
