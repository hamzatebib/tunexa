import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'

import { ErrorBox, Price, Spinner, StatusBadge } from '../components/ui'
import { api } from '../lib/api'
import { formatDate } from '../lib/format'
import { useLang } from '../lib/hooks'
import type { OrderSummary, Page } from '../lib/types'

export default function Orders() {
  const { t } = useTranslation()
  const lang = useLang()
  const orders = useQuery({ queryKey: ['orders'], queryFn: () => api.get<Page<OrderSummary>>('/orders?size=50') })

  if (orders.isLoading) return <Spinner />
  if (orders.error) return <ErrorBox error={orders.error} onRetry={() => orders.refetch()} />

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">{t('order.my_orders')}</h1>
      {orders.data?.items.length === 0 && (
        <div className="card p-8 text-center">
          <p className="mb-4">{t('order.no_orders')}</p>
          <Link to="/search" className="btn btn-primary">{t('cart.empty_cta')}</Link>
        </div>
      )}
      {orders.data?.items.map((o) => (
        <Link key={o.number} to={`/orders/${o.number}`} className="card flex flex-wrap items-center justify-between gap-3 p-4 hover:border-ink">
          <div className="space-y-1">
            <p className="font-semibold">
              <span className="ltr-nums">{t('order.number', { number: o.number })}</span>
            </p>
            <p className="text-sm text-gray-600">
              {t('order.placed_on', { date: formatDate(o.createdAt, lang, true) })} · {t('order.items_n', { count: o.itemCount })}
            </p>
            {o.firstItemTitle && <p className="line-clamp-1 text-sm text-gray-800">{o.firstItemTitle}</p>}
          </div>
          <div className="space-y-1 text-end">
            <StatusBadge status={o.status} />
            <Price value={o.total} className="block font-bold" />
          </div>
        </Link>
      ))}
    </div>
  )
}
