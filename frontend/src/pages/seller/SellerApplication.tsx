import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'

import { Field, FormError } from '../../components/ui'
import { api } from '../../lib/api'
import { localName } from '../../lib/format'
import { useGovernorates, useLang } from '../../lib/hooks'
import type { Seller } from '../../lib/types'

export default function SellerApplication() {
  const { t } = useTranslation()
  const lang = useLang()
  const queryClient = useQueryClient()
  const { data: governorates } = useGovernorates()
  const [form, setForm] = useState({ shopName: '', legalName: '', taxId: '', rneNumber: '', rib: '', pickupAddress: '', governorateId: '' })
  const set = (key: keyof typeof form) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const apply = useMutation({
    mutationFn: () => api.post<Seller>('/seller/apply', { ...form, governorateId: Number(form.governorateId) }),
    onSuccess: (s) => queryClient.setQueryData(['seller', 'me'], s),
  })

  return (
    <form
      className="card space-y-4 p-6"
      onSubmit={(e: FormEvent) => {
        e.preventDefault()
        apply.mutate()
      }}
    >
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t('seller.become')}</h1>
        <p className="text-sm text-gray-600">{t('seller.become_desc')}</p>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <Field label={t('seller.shop_name')}>
          <input className="input" value={form.shopName} onChange={set('shopName')} required />
        </Field>
        <Field label={t('seller.legal_name')}>
          <input className="input" value={form.legalName} onChange={set('legalName')} required />
        </Field>
        <Field label={t('seller.tax_id')} hint={t('seller.tax_id_hint')}>
          <input className="input ltr-nums uppercase" value={form.taxId} onChange={set('taxId')} required />
        </Field>
        <Field label={t('seller.rne')}>
          <input className="input ltr-nums" value={form.rneNumber} onChange={set('rneNumber')} required />
        </Field>
        <Field label={t('seller.rib')}>
          <input className="input ltr-nums" value={form.rib} onChange={set('rib')} pattern="\d{20}" maxLength={20} inputMode="numeric" required />
        </Field>
        <Field label={t('account.governorate')}>
          <select className="input" value={form.governorateId} onChange={set('governorateId')} required>
            <option value="">{t('account.choose')}</option>
            {governorates?.map((g) => (
              <option key={g.id} value={g.id}>{localName(g, lang)}</option>
            ))}
          </select>
        </Field>
        <div className="sm:col-span-2">
          <Field label={t('seller.pickup_address')}>
            <input className="input" value={form.pickupAddress} onChange={set('pickupAddress')} required />
          </Field>
        </div>
      </div>
      <FormError error={apply.error} />
      <button className="btn btn-primary" disabled={apply.isPending}>{t('seller.submit_application')}</button>
    </form>
  )
}
