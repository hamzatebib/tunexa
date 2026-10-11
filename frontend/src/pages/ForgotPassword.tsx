import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'

import { Field, FormError, Notice } from '../components/ui'
import { api } from '../lib/api'

export default function ForgotPassword() {
  const { t } = useTranslation()
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [devCode, setDevCode] = useState<string>()
  const [sent, setSent] = useState(false)

  const send = useMutation({
    mutationFn: () => api.post<{ devCode?: string }>('/auth/otp', { phone, purpose: 'RESET_PASSWORD' }),
    onSuccess: (res) => {
      setDevCode(res.devCode)
      setSent(true)
    },
  })
  const reset = useMutation({
    mutationFn: () => api.post<void>('/auth/reset-password', { phone, code, newPassword: password }),
  })

  return (
    <div className="mx-auto max-w-sm">
      <div className="card space-y-4 p-6">
        <h1 className="text-2xl font-bold text-gray-900">{t('auth.reset_title')}</h1>
        {reset.isSuccess ? (
          <>
            <Notice tone="success">{t('auth.reset_done')}</Notice>
            <Link to="/login" className="btn btn-primary w-full">{t('common.login')}</Link>
          </>
        ) : !sent ? (
          <form
            className="space-y-4"
            onSubmit={(e: FormEvent) => {
              e.preventDefault()
              send.mutate()
            }}
          >
            <Field label={t('auth.phone')} hint={t('auth.phone_hint')}>
              <input className="input ltr-nums" value={phone} onChange={(e) => setPhone(e.target.value)} inputMode="tel" required autoFocus />
            </Field>
            <FormError error={send.error} />
            <button className="btn btn-primary w-full" disabled={send.isPending}>{t('auth.send_code')}</button>
          </form>
        ) : (
          <form
            className="space-y-4"
            onSubmit={(e: FormEvent) => {
              e.preventDefault()
              reset.mutate()
            }}
          >
            <Notice>{t('auth.code_sent_to', { phone })}</Notice>
            {devCode && <Notice tone="warning">{t('order.dev_code', { code: devCode })}</Notice>}
            <Field label={t('auth.code')}>
              <input className="input ltr-nums tracking-[0.3em]" value={code} onChange={(e) => setCode(e.target.value)} inputMode="numeric" maxLength={6} required />
            </Field>
            <Field label={t('auth.new_password')} hint={t('auth.password_hint')}>
              <input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} minLength={8} autoComplete="new-password" required />
            </Field>
            <FormError error={reset.error} />
            <button className="btn btn-primary w-full" disabled={reset.isPending}>{t('auth.reset_btn')}</button>
          </form>
        )}
      </div>
    </div>
  )
}
