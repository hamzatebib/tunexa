import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'

import { Field, FormError, Notice } from '../../components/ui'
import { api } from '../../lib/api'
import { localName, parseTnd } from '../../lib/format'
import { flattenCategories, useCategories, useLang } from '../../lib/hooks'
import type { SellerOffer } from '../../lib/types'

export default function NewProductForm({ onCreated }: { onCreated: () => void }) {
  const { t } = useTranslation()
  const lang = useLang()
  const queryClient = useQueryClient()
  const { data: categories } = useCategories()
  const [form, setForm] = useState({
    categoryId: '', brandName: '', title: '', description: '', ean: '', imageUrl: '',
    price: '', compareAtPrice: '', stock: '10', handlingDays: '1',
  })
  const [priceError, setPriceError] = useState(false)
  const set = (key: keyof typeof form) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const create = useMutation({
    mutationFn: (body: unknown) => api.post<SellerOffer>('/seller/products', body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['seller', 'offers'] })
      onCreated()
    },
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    const price = parseTnd(form.price)
    const compareAtPrice = form.compareAtPrice ? parseTnd(form.compareAtPrice) : null
    if (!price || (form.compareAtPrice && !compareAtPrice)) {
      setPriceError(true)
      return
    }
    setPriceError(false)
    create.mutate({
      categoryId: Number(form.categoryId),
      brandName: form.brandName || null,
      title: form.title,
      description: form.description,
      ean: form.ean || null,
      imageUrl: form.imageUrl || null,
      offer: { price, compareAtPrice, stock: Number(form.stock), handlingDays: Number(form.handlingDays) },
    })
  }

  return (
    <form onSubmit={submit} className="card grid gap-3 p-5 sm:grid-cols-2">
      <Field label={t('seller.category')}>
        <select className="input" value={form.categoryId} onChange={set('categoryId')} required>
          <option value="">{t('account.choose')}</option>
          {categories &&
            flattenCategories(categories).map(({ node, depth }) => (
              <option key={node.id} value={node.id}>
                {'  '.repeat(depth)}
                {localName(node, lang)}
              </option>
            ))}
        </select>
      </Field>
      <Field label={`${t('seller.brand')} (${t('common.optional')})`}>
        <input className="input" value={form.brandName} onChange={set('brandName')} />
      </Field>
      <div className="sm:col-span-2">
        <Field label={t('seller.title_field')}>
          <input className="input" value={form.title} onChange={set('title')} maxLength={200} required />
        </Field>
      </div>
      <div className="sm:col-span-2">
        <Field label={t('seller.description')}>
          <textarea className="input min-h-28" value={form.description} onChange={set('description')} maxLength={4000} required />
        </Field>
      </div>
      <Field label={`${t('seller.ean')} (${t('common.optional')})`}>
        <input className="input ltr-nums" value={form.ean} onChange={set('ean')} pattern="\d{8,14}" inputMode="numeric" />
      </Field>
      <Field label={`${t('seller.image_url')} (${t('common.optional')})`}>
        <input className="input ltr-nums" type="url" value={form.imageUrl} onChange={set('imageUrl')} />
      </Field>
      <Field label={t('seller.price')}>
        <input className="input ltr-nums" value={form.price} onChange={set('price')} inputMode="decimal" placeholder="549,900" required />
      </Field>
      <Field label={`${t('seller.compare_price')} (${t('common.optional')})`}>
        <input className="input ltr-nums" value={form.compareAtPrice} onChange={set('compareAtPrice')} inputMode="decimal" />
      </Field>
      <Field label={t('seller.stock')}>
        <input className="input ltr-nums" type="number" min={0} value={form.stock} onChange={set('stock')} required />
      </Field>
      <Field label={t('seller.handling_days')}>
        <input className="input ltr-nums" type="number" min={0} max={30} value={form.handlingDays} onChange={set('handlingDays')} required />
      </Field>
      <div className="space-y-2 sm:col-span-2">
        {priceError && <Notice tone="warning">{t('seller.invalid_price')}</Notice>}
        <FormError error={create.error} />
        <button className="btn btn-primary" disabled={create.isPending}>{t('seller.create_product')}</button>
      </div>
    </form>
  )
}
