import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation, useParams } from 'react-router-dom'

import { ErrorBox, FormError, Notice, Price, Spinner, StatusBadge } from '../components/ui'
import { api } from '../lib/api'
import { formatDate, formatPhone } from '../lib/format'
import { useLang } from '../lib/hooks'
import type { OrderDetail } from '../lib/types'

function ConfirmBox({ order, initialDevCode, onConfirmed }: { order: OrderDetail; initialDevCode?: string; onConfirmed: () => void }) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [code, setCode] = useState('')
  const [devCode, setDevCode] = useState(initialDevCode)
  const [resent, setResent] = useState(false)

  const confirm = useMutation({
    mutationFn: () => api.post<OrderDetail>(`/orders/${order.number}/confirm`, { code }),
    onSuccess: (data) => {
      queryClient.setQueryData(['order', order.number], data)
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      onConfirmed()
    },
  })
  const resend = useMutation({
    mutationFn: () => api.post<{ devCode?: string }>(`/orders/${order.number}/resend-confirmation`),
    onSuccess: (data) => {
      setDevCode(data.devCode)
      setResent(true)
    },
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    confirm.mutate()
  }

  return (
    <section className="card border-amber-300 bg-amber-50/60 p-5">
      <h2 className="text-lg font-bold text-gray-900">{t('order.confirm_title')}</h2>
      <p className="mt-1 text-sm text-gray-700">
        {t('order.confirm_desc', { phone: formatPhone(order.shippingAddress.phone) })}
      </p>
      <p className="text-xs text-gray-600">{t('order.confirm_deadline')}</p>
      {devCode && (
        <div className="mt-3">
          <Notice>{t('order.dev_code', { code: devCode })}</Notice>
        </div>
      )}
      <form onSubmit={submit} className="mt-4 flex flex-wrap items-end gap-3">
        <label className="block">
          <span className="label">{t('order.code')}</span>
          <input
            className="input ltr-nums w-40 text-center text-lg tracking-[0.4em]"
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
            inputMode="numeric"
            autoComplete="one-time-code"
            required
          />
        </label>
        <button className="btn btn-primary" disabled={code.length !== 6 || confirm.isPending}>
          {t('order.confirm_btn')}
        </button>
        <button type="button" className="text-sm font-semibold text-ink-soft hover:underline" onClick={() => resend.mutate()} disabled={resend.isPending}>
          {t('order.resend')}
        </button>
      </form>
      <div className="mt-3 space-y-2">
        {resent && <Notice tone="success">{t('order.code_sent')}</Notice>}
        <FormError error={confirm.error ?? resend.error} />
      </div>
    </section>
  )
}

export default function OrderPage() {
  const { number } = useParams()
  const { t } = useTranslation()
  const lang = useLang()
  const location = useLocation()
  const queryClient = useQueryClient()
  const devCode = (location.state as { devCode?: string } | null)?.devCode
  const [justConfirmed, setJustConfirmed] = useState(false)

  const order = useQuery({ queryKey: ['order', number], queryFn: () => api.get<OrderDetail>(`/orders/${number}`) })
  const cancel = useMutation({
    mutationFn: () => api.post<OrderDetail>(`/orders/${number}/cancel`, {}),
    onSuccess: (data) => {
      queryClient.setQueryData(['order', number], data)
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
  })

  if (order.isLoading) return <Spinner />
  if (order.error || !order.data) return <ErrorBox error={order.error} onRetry={() => order.refetch()} />
  const o = order.data
  const a = o.shippingAddress

  return (
    <div className="mx-auto max-w-4xl space-y-5">
      <Link to="/orders" className="text-sm text-ink-soft hover:underline">← {t('order.my_orders')}</Link>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            <span className="ltr-nums">{t('order.number', { number: o.number })}</span>
          </h1>
          <p className="text-sm text-gray-600">{t('order.placed_on', { date: formatDate(o.createdAt, lang, true) })}</p>
        </div>
        <StatusBadge status={o.status} />
      </div>

      {o.status === 'PENDING_CONFIRMATION' && <ConfirmBox order={o} initialDevCode={devCode} onConfirmed={() => setJustConfirmed(true)} />}
      {justConfirmed && (
        <Notice tone="success">
          <strong>{t('order.thanks')}</strong> {t('order.thanks_desc')}
        </Notice>
      )}

      <div className="grid gap-5 md:grid-cols-[1fr_300px]">
        <section className="space-y-4">
          {o.packages.map((p) => (
            <div key={p.id} className="card">
              <div className="flex flex-wrap items-center justify-between gap-2 border-b border-gray-100 px-4 py-3">
                <div className="text-sm">
                  <p className="font-semibold">{t('order.package_by', { shop: p.shopName })}</p>
                  {p.trackingNumber && <p className="ltr-nums text-gray-600">{t('order.tracking', { number: p.trackingNumber })}</p>}
                </div>
                <StatusBadge status={p.status} />
              </div>
              <ul className="divide-y divide-gray-100">
                {p.items.map((i) => (
                  <li key={i.offerId} className="flex justify-between gap-3 px-4 py-3 text-sm">
                    <div>
                      <Link to={`/products/${i.productId}`} className="font-medium hover:underline">{i.title}</Link>
                      <p className="text-gray-600">{t('order.qty', { n: i.quantity })}</p>
                    </div>
                    <Price value={i.lineTotal} className="font-semibold" />
                  </li>
                ))}
              </ul>
            </div>
          ))}

          <div className="card p-4">
            <h2 className="mb-2 font-bold">{t('order.history')}</h2>
            <ol className="space-y-2 border-s-2 border-gray-200 ps-4 text-sm">
              {o.events.map((e, idx) => (
                <li key={idx}>
                  <span className="font-semibold">{t(`status.${e.status}`)}</span>
                  <span className="text-gray-500"> · {formatDate(e.createdAt, lang, true)}</span>
                  {e.note && <p className="text-gray-600">{e.note}</p>}
                </li>
              ))}
            </ol>
          </div>
        </section>

        <aside className="space-y-4">
          <div className="card space-y-1 p-4 text-sm">
            <h2 className="mb-1 font-bold">{t('order.shipping_address')}</h2>
            <p className="font-semibold">{a.fullName}</p>
            <p className="ltr-nums">{formatPhone(a.phone)}</p>
            <p>{a.street}</p>
            <p>
              {a.locality ? `${a.locality}, ` : ''}
              {a.delegation}
            </p>
            <p>
              <span className="ltr-nums">{a.postalCode}</span> {a.governorate}
            </p>
            {a.landmark && <p className="text-gray-500">{a.landmark}</p>}
          </div>
          <div className="card space-y-1 p-4 text-sm">
            <h2 className="mb-1 font-bold">{t('order.payment')}</h2>
            <p>💵 {t('order.payment_cod')}</p>
            <dl className="mt-2 space-y-1">
              <div className="flex justify-between"><dt>{t('checkout.items')}</dt><dd><Price value={o.itemsTotal} /></dd></div>
              <div className="flex justify-between">
                <dt>{t('checkout.shipping')}</dt>
                <dd>{o.shippingTotal === 0 ? t('checkout.free') : <Price value={o.shippingTotal} />}</dd>
              </div>
              <div className="flex justify-between"><dt>{t('checkout.stamp')}</dt><dd><Price value={o.stampDuty} /></dd></div>
              <div className="flex justify-between border-t border-gray-100 pt-1 font-bold">
                <dt>{t('checkout.total')}</dt>
                <dd><Price value={o.total} /></dd>
              </div>
            </dl>
          </div>
          {o.cancellable && (
            <button
              className="btn btn-danger w-full"
              disabled={cancel.isPending}
              onClick={() => window.confirm(t('order.cancel_confirm')) && cancel.mutate()}
            >
              {t('order.cancel_order')}
            </button>
          )}
          <FormError error={cancel.error} />
        </aside>
      </div>
    </div>
  )
}
