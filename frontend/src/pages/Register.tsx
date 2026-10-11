import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'

import { useAuth } from '../auth/context'
import { Field, FormError, Notice } from '../components/ui'
import { api } from '../lib/api'
import { useLang } from '../lib/hooks'
import { safeNext } from '../lib/nav'
import type { AuthResponse } from '../lib/types'

export default function Register() {
  const { t } = useTranslation()
  const lang = useLang()
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [step, setStep] = useState<'phone' | 'details'>('phone')
  const [devCode, setDevCode] = useState<string>()
  const [form, setForm] = useState({ phone: '', code: '', firstName: '', lastName: '', password: '', acceptTerms: false })
  const set = (key: keyof typeof form) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const sendCode = useMutation({
    mutationFn: () => api.post<{ devCode?: string }>('/auth/otp', { phone: form.phone, purpose: 'REGISTER' }),
    onSuccess: (res) => {
      setDevCode(res.devCode)
      setStep('details')
    },
  })
  const register = useMutation({
    mutationFn: () => api.post<AuthResponse>('/auth/register', { ...form, language: lang }),
    onSuccess: (auth) => {
      signIn(auth)
      navigate(safeNext(params.get('next')), { replace: true })
    },
  })

  return (
    <div className="mx-auto max-w-sm space-y-4">
      <div className="card space-y-4 p-6">
        <h1 className="text-2xl font-bold text-gray-900">{t('auth.register_title')}</h1>
        {step === 'phone' ? (
          <form
            className="space-y-4"
            onSubmit={(e: FormEvent) => {
              e.preventDefault()
              sendCode.mutate()
            }}
          >
            <Field label={t('auth.phone')} hint={t('auth.phone_hint')}>
              <input className="input ltr-nums" value={form.phone} onChange={set('phone')} inputMode="tel" autoComplete="tel" required autoFocus />
            </Field>
            <FormError error={sendCode.error} />
            <button className="btn btn-primary w-full" disabled={sendCode.isPending}>{t('auth.send_code')}</button>
          </form>
        ) : (
          <form
            className="space-y-3"
            onSubmit={(e: FormEvent) => {
              e.preventDefault()
              register.mutate()
            }}
          >
            <Notice>
              {t('auth.code_sent_to', { phone: form.phone })} ·{' '}
              <button type="button" className="font-semibold underline" onClick={() => setStep('phone')}>
                {t('auth.change_number')}
              </button>
            </Notice>
            {devCode && <Notice tone="warning">{t('order.dev_code', { code: devCode })}</Notice>}
            <Field label={t('auth.code')}>
              <input className="input ltr-nums tracking-[0.3em]" value={form.code} onChange={set('code')} inputMode="numeric" maxLength={6} autoComplete="one-time-code" required />
            </Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label={t('auth.first_name')}>
                <input className="input" value={form.firstName} onChange={set('firstName')} autoComplete="given-name" required />
              </Field>
              <Field label={t('auth.last_name')}>
                <input className="input" value={form.lastName} onChange={set('lastName')} autoComplete="family-name" required />
              </Field>
            </div>
            <Field label={t('auth.password')} hint={t('auth.password_hint')}>
              <input className="input" type="password" value={form.password} onChange={set('password')} minLength={8} autoComplete="new-password" required />
            </Field>
            <label className="flex items-start gap-2 text-sm">
              <input type="checkbox" className="mt-1" checked={form.acceptTerms} onChange={(e) => setForm((f) => ({ ...f, acceptTerms: e.target.checked }))} required />
              {t('auth.accept_terms')}
            </label>
            <FormError error={register.error} />
            <button className="btn btn-primary w-full" disabled={register.isPending}>{t('auth.create_account')}</button>
          </form>
        )}
      </div>
      <div className="card p-4 text-center text-sm">
        {t('auth.have_account')}{' '}
        <Link to="/login" className="font-semibold text-ink-soft hover:underline">{t('common.login')}</Link>
      </div>
    </div>
  )
}
