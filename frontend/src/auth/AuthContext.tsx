import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'

import { api, getToken, setToken } from '../lib/api'
import type { AuthResponse, User } from '../lib/types'
import { AuthContext } from './context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [ready, setReady] = useState(() => !getToken())
  const queryClient = useQueryClient()

  const signOut = useCallback(() => {
    setToken(null)
    setUser(null)
    queryClient.clear()
  }, [queryClient])

  const signIn = useCallback((auth: AuthResponse) => {
    setToken(auth.token)
    setUser(auth.user)
    queryClient.invalidateQueries()
  }, [queryClient])

  /** Récupère un jeton à jour (ex. le rôle vendeur vient d'être accordé). */
  const refresh = useCallback(async () => {
    const auth = await api.post<AuthResponse>('/me/token')
    signIn(auth)
  }, [signIn])

  useEffect(() => {
    if (!getToken()) return
    api.get<User>('/me')
      .then(setUser)
      .catch(() => setToken(null))
      .finally(() => setReady(true))
  }, [])

  useEffect(() => {
    const onLogout = () => setUser(null)
    window.addEventListener('tunexa:logout', onLogout)
    return () => window.removeEventListener('tunexa:logout', onLogout)
  }, [])

  const value = useMemo(() => ({ user, ready, signIn, signOut, refresh }), [user, ready, signIn, signOut, refresh])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

