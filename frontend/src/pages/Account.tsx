import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'

import { useAuth } from '../auth/context'
import AddressForm from '../components/AddressForm'
import { ErrorBox, Field, FormError, Notice, Spinner } from '../components/ui'
import { api } from '../lib/api'
import { formatPhone } from '../lib/format'
import type { Address, Lang, User } from '../lib/types'

function ProfileForm({ user }: { user: User }) {
  const { t, i18n } = useTranslation()
  const { refresh } = useAuth()
  const [form, setForm] = useState({ firstName: user.firstName, lastName: user.lastName, email: user.email ?? '', language: user.language })
  const save = useMutation({
    mutationFn: () => api.put<User>('/me', form),
    onSuccess: async (u) => {
      i18n.changeLanguage(u.language)
      await refresh()
    },
  })
  return (
    <form
      className="grid gap-3 sm:grid-cols-2"
      onSubmit={(e: FormEvent) => {
        e.preventDefault()
        save.mutate()
      }}
    >
      <Field label={t('auth.first_name')}>
        <input className="input" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} required />
      </Field>
      <Field label={t('auth.last_name')}>
        <input className="input" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} required />
      </Field>
      <Field label={t('auth.phone')}>
        <input className="input ltr-nums bg-gray-50" value={formatPhone(user.phone)} disabled />
      </Field>
      <Field label={`${t('account.email')} (${t('common.optional')})`}>
        <input className="input" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
      </Field>
      <Field label={t('account.language')}>
        <select className="input" value={form.language} onChange={(e) => setForm({ ...form, language: e.target.value as Lang })}>
          <option value="fr">Français</option>
          <option value="ar">العربية</option>
          <option value="en">English</option>
        </select>
      </Field>
      <div className="flex items-end">
        <button className="btn btn-dark" disabled={save.isPending}>{t('common.save')}</button>
      </div>
      <div className="sm:col-span-2">
        {save.isSuccess && <Notice tone="success">{t('account.saved')}</Notice>}
        <FormError error={save.error} />
      </div>
    </form>
  )
}

export default function Account() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const addresses = useQuery({ queryKey: ['addresses'], queryFn: () => api.get<Address[]>('/me/addresses') })
  const [editing, setEditing] = useState<Address | 'new' | null>(null)
  const remove = useMutation({
    mutationFn: (id: number) => api.delete<void>(`/me/addresses/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['addresses'] }),
  })

  if (!user) return <Spinner />

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">{t('account.title')}</h1>
      <section className="card p-5">
        <h2 className="mb-4 font-bold">{t('account.profile')}</h2>
        <ProfileForm user={user} />
      </section>

      <section className="card p-5">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-bold">{t('account.addresses')}</h2>
          {editing === null && (
            <button className="btn btn-ghost" onClick={() => setEditing('new')}>+ {t('account.add_address')}</button>
          )}
        </div>
        {editing !== null ? (
          <AddressForm
            initial={editing === 'new' ? undefined : editing}
            defaultName={`${user.firstName} ${user.lastName}`}
            defaultPhone={user.phone}
            onSaved={() => setEditing(null)}
            onCancel={() => setEditing(null)}
          />
        ) : (
          <>
            {addresses.isLoading && <Spinner />}
            {addresses.error && <ErrorBox error={addresses.error} />}
            {addresses.data?.length === 0 && <p className="text-sm text-gray-600">{t('account.no_address')}</p>}
            <div className="grid gap-3 sm:grid-cols-2">
              {addresses.data?.map((a) => (
                <div key={a.id} className="rounded-lg border border-gray-200 p-3 text-sm">
                  <p className="font-semibold">
                    {a.label}
                    {a.defaultAddress && <span className="ms-2 rounded bg-ink px-1.5 py-0.5 text-xs text-white">{t('account.default')}</span>}
                  </p>
                  <p>{a.fullName} · <span className="ltr-nums">{formatPhone(a.phone)}</span></p>
                  <p>{a.street}, {a.delegation}</p>
                  <p><span className="ltr-nums">{a.postalCode}</span> {a.governorateName}</p>
                  <div className="mt-2 flex gap-3">
                    <button className="font-semibold text-ink-soft hover:underline" onClick={() => setEditing(a)}>{t('common.edit')}</button>
                    <button className="text-red-700 hover:underline" onClick={() => remove.mutate(a.id)}>{t('common.delete')}</button>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </section>
    </div>
  )
}
