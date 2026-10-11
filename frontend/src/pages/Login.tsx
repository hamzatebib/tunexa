import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'

import { useAuth } from '../auth/context'
import { Field, FormError } from '../components/ui'
import { api } from '../lib/api'
import { safeNext } from '../lib/nav'
import type { AuthResponse } from '../lib/types'

export default function Login() {
  const { t, i18n } = useTranslation()
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')

  const login = useMutation({
    mutationFn: () => api.post<AuthResponse>('/auth/login', { phone, password }),
    onSuccess: (auth) => {
      signIn(auth)
      i18n.changeLanguage(auth.user.language)
      navigate(safeNext(params.get('next')), { replace: true })
    },
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    login.mutate()
  }

  return (
    <div className="mx-auto max-w-sm space-y-4">
      <form onSubmit={submit} className="card space-y-4 p-6">
        <h1 className="text-2xl font-bold text-gray-900">{t('auth.login_title')}</h1>
        <Field label={t('auth.phone')} hint={t('auth.phone_hint')}>
          <input className="input ltr-nums" value={phone} onChange={(e) => setPhone(e.target.value)} inputMode="tel" autoComplete="tel" required autoFocus />
        </Field>
        <Field label={t('auth.password')}>
          <input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
        </Field>
        <FormError error={login.error} />
        <button className="btn btn-primary w-full" disabled={login.isPending}>{t('auth.login_btn')}</button>
        <Link to="/forgot-password" className="block text-center text-sm text-ink-soft hover:underline">{t('auth.forgot')}</Link>
      </form>
      <div className="card p-4 text-center text-sm">
        {t('auth.no_account')}{' '}
        <Link to={`/register${params.get('next') ? `?next=${encodeURIComponent(params.get('next')!)}` : ''}`} className="font-semibold text-ink-soft hover:underline">
          {t('common.register')}
        </Link>
      </div>
      {import.meta.env.DEV && (
        <div className="rounded-lg border border-dashed border-gray-300 p-3 text-xs text-gray-600">
          <p className="mb-1 font-semibold">{t('auth.demo_accounts')}</p>
          <p className="ltr-nums">Client : 55 555 555 / Client12345</p>
          <p className="ltr-nums">Vendeur : 22 222 222 / Vendeur12345</p>
          <p className="ltr-nums">Admin : 20 000 000 / Admin12345</p>
        </div>
      )}
    </div>
  )
}
