import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Navigate, useNavigate } from 'react-router-dom'

import { useAuth } from '../auth/context'
import AddressForm from '../components/AddressForm'
import { ErrorBox, FormError, Notice, Price, Spinner } from '../components/ui'
import { api } from '../lib/api'
import { formatDate, formatPhone, formatTnd } from '../lib/format'
import { useCart, useLang } from '../lib/hooks'
import type { Address, OrderDetail, Quote } from '../lib/types'

export default function Checkout() {
  const { t } = useTranslation()
  const lang = useLang()
  const { user } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const cart = useCart()
  const addresses = useQuery({ queryKey: ['addresses'], queryFn: () => api.get<Address[]>('/me/addresses') })
  const [selected, setSelected] = useState<number | null>(null)
  const [adding, setAdding] = useState(false)
  const addressId = selected ?? addresses.data?.find((a) => a.defaultAddress)?.id ?? addresses.data?.[0]?.id ?? null

  const quote = useQuery({
    queryKey: ['quote', addressId, cart.data?.itemsTotal, cart.data?.itemCount],
    queryFn: () => api.get<Quote>(`/checkout/quote?addressId=${addressId}`),
    enabled: addressId !== null && !!cart.data?.itemCount,
  })

  const place = useMutation({
    mutationFn: () =>
      api.post<{ order: OrderDetail; devConfirmationCode?: string }>('/checkout', { addressId, paymentMethod: 'COD' }),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['cart'] })
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      navigate(`/orders/${res.order.number}`, { state: { devCode: res.devConfirmationCode } })
    },
  })

  if (cart.isLoading || addresses.isLoading) return <Spinner />
  if (cart.error) return <ErrorBox error={cart.error} />
  if (cart.data && cart.data.itemCount === 0 && !place.isSuccess) return <Navigate to="/cart" replace />

  const q = quote.data

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
      <div className="space-y-6">
        <h1 className="text-2xl font-bold text-gray-900">{t('checkout.title')}</h1>

        <section className="card p-4">
          <h2 className="mb-3 font-bold">{t('checkout.step_address')}</h2>
          {addresses.data?.length === 0 && !adding && <p className="mb-3 text-sm text-gray-600">{t('checkout.no_address')}</p>}
          <div className="space-y-2">
            {addresses.data?.map((a) => (
              <label key={a.id} className={`flex cursor-pointer gap-3 rounded-lg border p-3 text-sm ${addressId === a.id ? 'border-ink bg-ink/5' : 'border-gray-200'}`}>
                <input type="radio" name="address" checked={addressId === a.id} onChange={() => setSelected(a.id)} />
                <span>
                  <span className="font-semibold">{a.fullName}</span> · <span className="ltr-nums">{formatPhone(a.phone)}</span>
                  <br />
                  {a.street}, {a.locality ? `${a.locality}, ` : ''}
                  {a.delegation}, <span className="ltr-nums">{a.postalCode}</span> {a.governorateName}
                  {a.landmark && <span className="block text-gray-500">{a.landmark}</span>}
                </span>
              </label>
            ))}
          </div>
          {adding ? (
            <div className="mt-4 border-t border-gray-100 pt-4">
              <AddressForm
                defaultName={user ? `${user.firstName} ${user.lastName}` : ''}
                defaultPhone={user?.phone}
                onSaved={(a) => {
                  setSelected(a.id)
                  setAdding(false)
                }}
                onCancel={addresses.data?.length ? () => setAdding(false) : undefined}
              />
            </div>
          ) : (
            <button className="btn btn-ghost mt-3" onClick={() => setAdding(true)}>
              + {t('checkout.add_address')}
            </button>
          )}
        </section>

        <section className="card p-4">
          <h2 className="mb-3 font-bold">{t('checkout.step_payment')}</h2>
          <label className="flex gap-3 rounded-lg border border-ink bg-ink/5 p-3 text-sm">
            <input type="radio" checked readOnly />
            <span>
              <span className="font-semibold">💵 {t('checkout.cod')}</span>
              <span className="block text-gray-600">{t('checkout.cod_desc')}</span>
            </span>
          </label>
          <p className="mt-2 text-xs text-gray-500">{t('checkout.card_soon')}</p>
          {q && !q.codAllowed && (
            <div className="mt-3">
              <Notice tone="warning">{t('checkout.cod_limit', { amount: formatTnd(q.codMaxTotal, lang) })}</Notice>
            </div>
          )}
        </section>

        {q && (
          <section className="card p-4">
            <h2 className="mb-3 font-bold">{t('checkout.step_review')}</h2>
            <div className="space-y-3">
              {q.packages.map((p, i) => (
                <div key={p.sellerId} className="rounded-lg bg-gray-50 p-3 text-sm">
                  <p className="font-semibold">{t('checkout.package_n', { n: i + 1, shop: p.shopName })}</p>
                  <p className="text-gray-600">
                    {t('checkout.delivery_between', { from: formatDate(p.deliveryFrom, lang), to: formatDate(p.deliveryTo, lang) })}
                  </p>
                  <ul className="mt-2 space-y-1">
                    {cart.data?.groups.find((g) => g.sellerId === p.sellerId)?.lines.map((l) => (
                      <li key={l.offerId} className="flex justify-between gap-3">
                        <span>
                          {l.quantity} × {l.title}
                        </span>
                        <Price value={l.lineTotal} />
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          </section>
        )}
      </div>

      <aside className="card h-fit space-y-3 p-4 lg:sticky lg:top-4">
        <h2 className="font-bold">{t('checkout.summary')}</h2>
        {quote.isLoading && <Spinner />}
        {quote.error && <ErrorBox error={quote.error} />}
        {q && (
          <dl className="space-y-1 text-sm">
            <div className="flex justify-between">
              <dt>{t('checkout.items')}</dt>
              <dd><Price value={q.itemsTotal} /></dd>
            </div>
            <div className="flex justify-between">
              <dt>{t('checkout.shipping')}</dt>
              <dd>{q.shippingTotal === 0 ? <span className="font-semibold text-emerald-700">{t('checkout.free')}</span> : <Price value={q.shippingTotal} />}</dd>
            </div>
            <div className="flex justify-between">
              <dt>{t('checkout.stamp')}</dt>
              <dd><Price value={q.stampDuty} /></dd>
            </div>
            {q.codFee > 0 && (
              <div className="flex justify-between">
                <dt>{t('checkout.cod_fee')}</dt>
                <dd><Price value={q.codFee} /></dd>
              </div>
            )}
            <div className="flex justify-between border-t border-gray-200 pt-2 text-lg font-bold">
              <dt>{t('checkout.total')}</dt>
              <dd><Price value={q.total} /></dd>
            </div>
            <p className="text-end text-xs text-gray-500">{t('checkout.vat_included', { amount: formatTnd(q.vatIncluded, lang) })}</p>
          </dl>
        )}
        <FormError error={place.error} />
        <button
          className="btn btn-primary w-full py-3 text-base"
          disabled={!q || !q.codAllowed || !cart.data?.checkoutAllowed || place.isPending}
          onClick={() => place.mutate()}
        >
          {t('checkout.place_order')}
        </button>
        <p className="text-xs text-gray-500">{t('checkout.terms')}</p>
      </aside>
    </div>
  )
}
