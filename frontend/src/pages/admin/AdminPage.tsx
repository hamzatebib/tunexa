import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'

import { ErrorBox, FormError, Price, ProductImage, Spinner, StatusBadge } from '../../components/ui'
import { api } from '../../lib/api'
import { formatDate, formatTnd } from '../../lib/format'
import { useLang } from '../../lib/hooks'
import type { AdminStats, OrderDetail, OrderStatus, Page, PendingProduct, Seller } from '../../lib/types'

function StatTile({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="card p-4">
      <p className="text-xs font-medium text-gray-500">{label}</p>
      <p className="mt-1 text-2xl font-bold text-gray-900 ltr-nums">{value}</p>
    </div>
  )
}

function useAdminAction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ path, body }: { path: string; body?: unknown }) => api.post<unknown>(`/admin/${path}`, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin'] }),
  })
}

function askReason(t: (k: string) => string): string | null {
  const reason = window.prompt(t('admin.reject_reason'))
  return reason && reason.trim() ? reason.trim() : null
}

export default function AdminPage() {
  const { t } = useTranslation()
  const lang = useLang()
  const stats = useQuery({ queryKey: ['admin', 'stats'], queryFn: () => api.get<AdminStats>('/admin/stats') })
  const sellers = useQuery({ queryKey: ['admin', 'sellers'], queryFn: () => api.get<Seller[]>('/admin/sellers?status=PENDING') })
  const products = useQuery({ queryKey: ['admin', 'products'], queryFn: () => api.get<PendingProduct[]>('/admin/products/pending') })
  const orders = useQuery({ queryKey: ['admin', 'orders'], queryFn: () => api.get<Page<OrderDetail>>('/admin/orders?size=20') })
  const action = useAdminAction()

  const delivery = (packageId: number, status: OrderStatus) =>
    action.mutate({ path: `packages/${packageId}/delivery`, body: { status } })

  if (stats.isLoading) return <Spinner />
  if (stats.error) return <ErrorBox error={stats.error} onRetry={() => stats.refetch()} />
  const s = stats.data!

  return (
    <div className="space-y-8">
      <h1 className="text-2xl font-bold text-gray-900">{t('admin.dashboard')}</h1>
      <section className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <StatTile label={t('admin.orders_today')} value={s.ordersToday} />
        <StatTile label={t('admin.revenue_today')} value={formatTnd(s.revenueToday, lang)} />
        <StatTile label={t('admin.orders_30')} value={s.ordersLast30Days} />
        <StatTile label={t('admin.revenue_30')} value={formatTnd(s.revenueLast30Days, lang)} />
        <StatTile label={t('admin.pending_sellers')} value={s.pendingSellers} />
        <StatTile label={t('admin.pending_products')} value={s.pendingProducts} />
        <StatTile label={t('admin.to_ship')} value={s.packagesToShip} />
        <StatTile label={t('admin.customers')} value={s.customers} />
      </section>

      <FormError error={action.error} />

      <section className="space-y-3">
        <h2 className="text-lg font-bold">{t('admin.sellers_to_review')}</h2>
        {sellers.data?.length === 0 && <p className="text-sm text-gray-600">{t('admin.nothing_to_review')}</p>}
        {sellers.data?.map((sl) => (
          <div key={sl.id} className="card flex flex-wrap items-center justify-between gap-3 p-4 text-sm">
            <div>
              <p className="font-semibold">{sl.shopName} <span className="font-normal text-gray-500">— {sl.legalName}</span></p>
              <p className="ltr-nums text-gray-600">MF {sl.taxId} · RNE {sl.rneNumber} · {sl.governorateName}</p>
              <p className="text-xs text-gray-500">{formatDate(sl.createdAt, lang, true)}</p>
            </div>
            <div className="flex gap-2">
              <button className="btn btn-primary" onClick={() => action.mutate({ path: `sellers/${sl.id}/approve` })}>{t('admin.approve')}</button>
              <button
                className="btn btn-danger"
                onClick={() => {
                  const reason = askReason(t)
                  if (reason) action.mutate({ path: `sellers/${sl.id}/reject`, body: { reason } })
                }}
              >
                {t('admin.reject')}
              </button>
            </div>
          </div>
        ))}
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-bold">{t('admin.products_to_review')}</h2>
        {products.data?.length === 0 && <p className="text-sm text-gray-600">{t('admin.nothing_to_review')}</p>}
        {products.data?.map((p) => (
          <div key={p.id} className="card flex flex-wrap gap-4 p-4 text-sm">
            <ProductImage src={p.imageUrl} title={p.title} className="h-20 w-20 shrink-0 rounded" />
            <div className="min-w-0 flex-1">
              <p className="font-semibold">{p.title}</p>
              <p className="text-gray-600">
                {p.category}
                {p.brand && ` · ${p.brand}`}
                {p.sellerShopName && ` · ${t('admin.by_seller', { shop: p.sellerShopName })}`}
              </p>
              <p className="line-clamp-2 text-gray-700">{p.description}</p>
              {p.price && <Price value={p.price} className="font-semibold" />}
            </div>
            <div className="flex items-start gap-2">
              <button className="btn btn-primary" onClick={() => action.mutate({ path: `products/${p.id}/approve` })}>{t('admin.approve')}</button>
              <button
                className="btn btn-danger"
                onClick={() => {
                  const reason = askReason(t)
                  if (reason) action.mutate({ path: `products/${p.id}/reject`, body: { reason } })
                }}
              >
                {t('admin.reject')}
              </button>
            </div>
          </div>
        ))}
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-bold">{t('admin.orders')}</h2>
        {orders.data?.items.map((o) => (
          <div key={o.number} className="card p-4 text-sm">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="font-semibold ltr-nums">{o.number}</p>
              <div className="flex items-center gap-3">
                <Price value={o.total} className="font-semibold" />
                <StatusBadge status={o.status} />
              </div>
            </div>
            <p className="text-gray-600">
              {o.shippingAddress.fullName} · {o.shippingAddress.governorate} · {formatDate(o.createdAt, lang, true)}
            </p>
            <ul className="mt-2 space-y-2">
              {o.packages.map((p) => (
                <li key={p.id} className="flex flex-wrap items-center justify-between gap-2 rounded bg-gray-50 px-3 py-2">
                  <span>
                    {p.shopName} · <StatusBadge status={p.status} />
                    {p.trackingNumber && <span className="ms-2 ltr-nums text-gray-500">{p.trackingNumber}</span>}
                  </span>
                  {p.status === 'SHIPPED' && (
                    <span className="flex gap-2">
                      <button className="btn btn-ghost px-2 py-1" onClick={() => delivery(p.id, 'DELIVERED')}>{t('admin.mark_delivered')}</button>
                      <button className="btn btn-danger px-2 py-1" onClick={() => delivery(p.id, 'DELIVERY_FAILED')}>{t('admin.mark_failed')}</button>
                    </span>
                  )}
                  {p.status === 'DELIVERY_FAILED' && (
                    <button className="btn btn-ghost px-2 py-1" onClick={() => delivery(p.id, 'RETURNED')}>{t('admin.mark_returned')}</button>
                  )}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </section>
    </div>
  )
}
