import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'

import { useAuth } from '../auth/context'
import type { Role } from '../lib/types'
import { Spinner } from './ui'

export default function RequireAuth({ children, role }: { children: ReactNode; role?: Role }) {
  const { user, ready } = useAuth()
  const location = useLocation()
  if (!ready) return <Spinner />
  if (!user) return <Navigate to={`/login?next=${encodeURIComponent(location.pathname + location.search)}`} replace />
  if (role && user.role !== role) return <Navigate to="/" replace />
  return <>{children}</>
}
