import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'

import { api } from '../lib/api'
import { localName } from '../lib/format'
import { useGovernorates, useLang } from '../lib/hooks'
import type { Address } from '../lib/types'
import { Field, FormError } from './ui'

interface Props {
  initial?: Address
  defaultName?: string
  defaultPhone?: string
  onSaved: (address: Address) => void
  onCancel?: () => void
}

export default function AddressForm({ initial, defaultName, defaultPhone, onSaved, onCancel }: Props) {
  const { t } = useTranslation()
  const lang = useLang()
  const { data: governorates } = useGovernorates()
  const queryClient = useQueryClient()
  const [form, setForm] = useState({
    label: initial?.label ?? t('account.label_placeholder').split(',')[0],
    fullName: initial?.fullName ?? defaultName ?? '',
    phone: initial?.phone ?? defaultPhone ?? '',
    governorateId: initial?.governorateId ? String(initial.governorateId) : '',
    delegation: initial?.delegation ?? '',
    locality: initial?.locality ?? '',
    postalCode: initial?.postalCode ?? '',
    street: initial?.street ?? '',
    landmark: initial?.landmark ?? '',
    defaultAddress: initial?.defaultAddress ?? false,
  })
  const set = (key: keyof typeof form) => (e: { target: { value: string } }) =>
    setForm((f) => ({ ...f, [key]: e.target.value }))

  const save = useMutation({
    mutationFn: () => {
      const body = { ...form, governorateId: Number(form.governorateId) }
      return initial
        ? api.put<Address>(`/me/addresses/${initial.id}`, body)
        : api.post<Address>('/me/addresses', body)
    },
    onSuccess: (address) => {
      queryClient.invalidateQueries({ queryKey: ['addresses'] })
      onSaved(address)
    },
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    save.mutate()
  }

  return (
    <form onSubmit={submit} className="grid gap-3 sm:grid-cols-2">
      <Field label={t('account.label')}>
        <input className="input" value={form.label} onChange={set('label')} placeholder={t('account.label_placeholder')} required />
      </Field>
      <Field label={t('account.full_name')}>
        <input className="input" value={form.fullName} onChange={set('fullName')} required autoComplete="name" />
      </Field>
      <Field label={t('account.contact_phone')} hint={t('auth.phone_hint')}>
        <input className="input ltr-nums" value={form.phone} onChange={set('phone')} required inputMode="tel" autoComplete="tel" />
      </Field>
      <Field label={t('account.governorate')}>
        <select className="input" value={form.governorateId} onChange={set('governorateId')} required>
          <option value="">{t('account.choose')}</option>
          {governorates?.map((g) => (
            <option key={g.id} value={g.id}>
              {localName(g, lang)}
            </option>
          ))}
        </select>
      </Field>
      <Field label={t('account.delegation')}>
        <input className="input" value={form.delegation} onChange={set('delegation')} required />
      </Field>
      <Field label={`${t('account.locality')} (${t('common.optional')})`}>
        <input className="input" value={form.locality} onChange={set('locality')} />
      </Field>
      <Field label={t('account.postal_code')}>
        <input className="input ltr-nums" value={form.postalCode} onChange={set('postalCode')} required pattern="\d{4}" inputMode="numeric" maxLength={4} />
      </Field>
      <Field label={t('account.street')}>
        <input className="input" value={form.street} onChange={set('street')} required autoComplete="street-address" />
      </Field>
      <div className="sm:col-span-2">
        <Field label={`${t('account.landmark')} (${t('common.optional')})`}>
          <input className="input" value={form.landmark} onChange={set('landmark')} placeholder={t('account.landmark_hint')} />
        </Field>
      </div>
      <label className="flex items-center gap-2 text-sm sm:col-span-2">
        <input
          type="checkbox"
          checked={form.defaultAddress}
          onChange={(e) => setForm((f) => ({ ...f, defaultAddress: e.target.checked }))}
        />
        {t('account.make_default')}
      </label>
      <div className="sm:col-span-2">
        <FormError error={save.error} />
      </div>
      <div className="flex gap-2 sm:col-span-2">
        <button className="btn btn-dark" disabled={save.isPending}>
          {t('common.save')}
        </button>
        {onCancel && (
          <button type="button" className="btn btn-ghost" onClick={onCancel}>
            {t('common.cancel')}
          </button>
        )}
      </div>
    </form>
  )
}
