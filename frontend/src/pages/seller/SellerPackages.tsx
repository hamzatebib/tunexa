import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import { ErrorBox, FormError, Spinner, StatusBadge } from '../../components/ui'
import { api } from '../../lib/api'
import { formatDate, formatPhone, formatTnd } from '../../lib/format'
import { useLang } from '../../lib/hooks'
import type { Page, SellerPackage } from '../../lib/types'

function PackageCard({ pkg }: { pkg: SellerPackage }) {
  const { t } = useTranslation()
  const lang = useLang()
  const queryClient = useQueryClient()
  const [tracking, setTracking] = useState('')
  const action = useMutation({
    mutationFn: ({ path, body }: { path: string; body?: unknown }) => api.post<SellerPackage>(`/seller/packages/${pkg.id}/${path}`, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['seller', 'packages'] }),
  })
  const a = pkg.shipTo

  return (
    <div className="card space-y-3 p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <p className="font-semibold ltr-nums">{pkg.orderNumber}</p>
          <p className="text-xs text-gray-500">{formatDate(pkg.orderedAt, lang, true)}</p>
        </div>
        <StatusBadge status={pkg.status} />
      </div>
      <ul className="text-sm">
        {pkg.items.map((i) => (
          <li key={i.offerId}>
            {i.quantity} × {i.title}
          </li>
        ))}
      </ul>
      <div className="grid gap-3 text-sm sm:grid-cols-2">
        <div className="rounded-lg bg-gray-50 p-3">
          <p className="font-semibold">{t('seller.ship_to')}</p>
          <p>{a.fullName} · <span className="ltr-nums">{formatPhone(a.phone)}</span></p>
          <p>{a.street}, {a.delegation}, <span className="ltr-nums">{a.postalCode}</span> {a.governorate}</p>
          {a.landmark && <p className="text-gray-500">{a.landmark}</p>}
        </div>
        <div className="rounded-lg bg-gray-50 p-3">
          <p className="font-semibold">{t('seller.to_collect', { amount: formatTnd(pkg.amountToCollect, lang) })}</p>
          <p>{t('seller.commission', { amount: formatTnd(pkg.commission, lang) })}</p>
          <p>{t('seller.net', { amount: formatTnd(pkg.netAmount, lang) })}</p>
          {pkg.trackingNumber && <p className="ltr-nums">{t('order.tracking', { number: pkg.trackingNumber })}</p>}
        </div>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        {pkg.status === 'CONFIRMED' && (
          <button className="btn btn-primary" onClick={() => action.mutate({ path: 'accept' })} disabled={action.isPending}>
            {t('seller.accept')}
          </button>
        )}
        {pkg.status === 'PROCESSING' && (
          <>
            <input className="input ltr-nums w-48" placeholder={t('seller.tracking_number')} value={tracking} onChange={(e) => setTracking(e.target.value)} />
            <button className="btn btn-primary" disabled={!tracking.trim() || action.isPending} onClick={() => action.mutate({ path: 'ship', body: { trackingNumber: tracking } })}>
              {t('seller.ship')}
            </button>
          </>
        )}
        {(pkg.status === 'CONFIRMED' || pkg.status === 'PROCESSING') && (
          <button className="btn btn-danger" onClick={() => window.confirm(t('order.cancel_confirm')) && action.mutate({ path: 'cancel', body: {} })} disabled={action.isPending}>
            {t('seller.refuse')}
          </button>
        )}
      </div>
      <FormError error={action.error} />
    </div>
  )
}

export default function SellerPackages() {
  const { t } = useTranslation()
  const [filter, setFilter] = useState<'todo' | 'all'>('todo')
  const packages = useQuery({
    queryKey: ['seller', 'packages', filter],
    queryFn: () => api.get<Page<SellerPackage>>(`/seller/packages?size=50${filter === 'todo' ? '&status=CONFIRMED&status=PROCESSING' : ''}`),
  })
  return (
    <div className="space-y-4">
      <div className="flex gap-2">
        {(['todo', 'all'] as const).map((f) => (
          <button key={f} className={`btn ${filter === f ? 'btn-dark' : 'btn-ghost'}`} onClick={() => setFilter(f)}>
            {f === 'todo' ? t('seller.filter_to_process') : t('seller.filter_all')}
          </button>
        ))}
      </div>
      {packages.isLoading && <Spinner />}
      {packages.error && <ErrorBox error={packages.error} onRetry={() => packages.refetch()} />}
      {packages.data?.items.length === 0 && <p className="card p-6 text-gray-600">{t('seller.no_packages')}</p>}
      {packages.data?.items.map((p) => <PackageCard key={p.id} pkg={p} />)}
    </div>
  )
}
