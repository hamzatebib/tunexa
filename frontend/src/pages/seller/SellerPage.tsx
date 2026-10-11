import { useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'

import { useAuth } from '../../auth/context'
import { ErrorBox, Notice, Spinner } from '../../components/ui'
import { api, ApiError } from '../../lib/api'
import type { Seller } from '../../lib/types'
import NewProductForm from './NewProductForm'
import SellerApplication from './SellerApplication'
import SellerOffers from './SellerOffers'
import SellerPackages from './SellerPackages'

type Tab = 'packages' | 'offers' | 'new'

export default function SellerPage() {
  const { t } = useTranslation()
  const { user, refresh } = useAuth()
  const [tab, setTab] = useState<Tab>('packages')
  const seller = useQuery({
    queryKey: ['seller', 'me'],
    queryFn: async () => {
      try {
        return await api.get<Seller>('/seller/me')
      } catch (e) {
        if (e instanceof ApiError && e.status === 404) return null
        throw e
      }
    },
  })

  const approved = seller.data?.status === 'APPROVED'
  // Le dossier vient d'être validé : on récupère un jeton portant le rôle vendeur
  useEffect(() => {
    if (approved && user?.role === 'CUSTOMER') refresh()
  }, [approved, user?.role, refresh])

  if (seller.isLoading) return <Spinner />
  if (seller.error) return <ErrorBox error={seller.error} onRetry={() => seller.refetch()} />

  const s = seller.data
  if (!s || s.status === 'REJECTED') {
    return (
      <div className="mx-auto max-w-2xl space-y-4">
        {s?.status === 'REJECTED' && (
          <Notice tone="warning">
            <strong>{t('seller.rejected_title')}</strong> — {t('seller.rejected_desc', { reason: s.rejectionReason })}
          </Notice>
        )}
        <SellerApplication />
      </div>
    )
  }
  if (s.status === 'PENDING') {
    return (
      <div className="card mx-auto max-w-xl p-8 text-center">
        <p className="text-4xl" aria-hidden>⏳</p>
        <h1 className="mt-3 text-xl font-bold">{t('seller.pending_title')}</h1>
        <p className="mt-2 text-gray-600">{t('seller.pending_desc', { shop: s.shopName })}</p>
      </div>
    )
  }
  if (user?.role !== 'SELLER') return <Spinner />

  const tabs: { key: Tab; label: string }[] = [
    { key: 'packages', label: t('seller.tab_packages') },
    { key: 'offers', label: t('seller.tab_offers') },
    { key: 'new', label: t('seller.tab_new') },
  ]

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t('seller.dashboard')}</h1>
        <p className="text-gray-600">{s.shopName}</p>
      </div>
      <div className="flex gap-1 border-b border-gray-200" role="tablist">
        {tabs.map((tb) => (
          <button
            key={tb.key}
            role="tab"
            aria-selected={tab === tb.key}
            onClick={() => setTab(tb.key)}
            className={`-mb-px border-b-2 px-4 py-2 text-sm font-semibold ${tab === tb.key ? 'border-ink text-ink' : 'border-transparent text-gray-500 hover:text-gray-800'}`}
          >
            {tb.label}
          </button>
        ))}
      </div>
      {tab === 'packages' && <SellerPackages />}
      {tab === 'offers' && <SellerOffers />}
      {tab === 'new' && <NewProductForm onCreated={() => setTab('offers')} />}
    </div>
  )
}
