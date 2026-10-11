import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'

import { ErrorBox, FormError, ProductImage, Spinner } from '../../components/ui'
import { api } from '../../lib/api'
import { parseTnd } from '../../lib/format'
import type { ProductStatus, SellerOffer } from '../../lib/types'

const STATUS_STYLE: Record<ProductStatus, string> = {
  PENDING_REVIEW: 'bg-amber-100 text-amber-900',
  PUBLISHED: 'bg-emerald-100 text-emerald-900',
  REJECTED: 'bg-red-100 text-red-900',
}

function OfferRow({ offer }: { offer: SellerOffer }) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [price, setPrice] = useState((offer.price / 1000).toFixed(3))
  const [stock, setStock] = useState(String(offer.stock))
  const [active, setActive] = useState(offer.active)

  const save = useMutation({
    mutationFn: () => {
      const millimes = parseTnd(price)
      if (!millimes) throw new Error(t('seller.invalid_price'))
      return api.put<SellerOffer>(`/seller/offers/${offer.offerId}`, {
        offer: {
          price: millimes,
          compareAtPrice: offer.compareAtPrice && offer.compareAtPrice > millimes ? offer.compareAtPrice : null,
          stock: Number(stock),
          handlingDays: offer.handlingDays,
        },
        active,
      })
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['seller', 'offers'] }),
  })

  return (
    <tr className="align-top">
      <td className="p-3">
        <div className="flex items-center gap-3">
          <ProductImage src={offer.imageUrl} title={offer.title} className="h-12 w-12 shrink-0 rounded" />
          <div>
            <p className="font-medium">{offer.title}</p>
            <span className={`mt-1 inline-block rounded-full px-2 py-0.5 text-xs font-semibold ${STATUS_STYLE[offer.productStatus]}`}>
              {t(`seller.product_status_${offer.productStatus}`)}
            </span>
            {offer.rejectionReason && <p className="text-xs text-red-700">{offer.rejectionReason}</p>}
          </div>
        </div>
      </td>
      <td className="p-3">
        <input className="input ltr-nums w-28" value={price} onChange={(e) => setPrice(e.target.value)} aria-label={t('seller.price')} />
      </td>
      <td className="p-3">
        <input className="input ltr-nums w-20" type="number" min={0} value={stock} onChange={(e) => setStock(e.target.value)} aria-label={t('seller.stock')} />
      </td>
      <td className="p-3">
        <label className="flex items-center gap-2 whitespace-nowrap">
          <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
          {active ? t('seller.active') : t('seller.paused')}
        </label>
      </td>
      <td className="p-3">
        <button className="btn btn-ghost" onClick={() => save.mutate()} disabled={save.isPending}>
          {t('seller.update')}
        </button>
        <FormError error={save.error} />
      </td>
    </tr>
  )
}

export default function SellerOffers() {
  const { t } = useTranslation()
  const offers = useQuery({ queryKey: ['seller', 'offers'], queryFn: () => api.get<SellerOffer[]>('/seller/offers') })
  if (offers.isLoading) return <Spinner />
  if (offers.error) return <ErrorBox error={offers.error} onRetry={() => offers.refetch()} />
  if (offers.data?.length === 0) return <p className="card p-6 text-gray-600">{t('seller.no_offers')}</p>
  return (
    <div className="card overflow-x-auto">
      <table className="w-full text-start text-sm">
        <thead className="bg-gray-50 text-xs text-gray-500 uppercase">
          <tr>
            <th className="p-3 text-start">{t('seller.title_field')}</th>
            <th className="p-3 text-start">{t('seller.price')}</th>
            <th className="p-3 text-start">{t('seller.stock')}</th>
            <th className="p-3 text-start" />
            <th className="p-3" />
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {offers.data?.map((o) => <OfferRow key={o.offerId} offer={o} />)}
        </tbody>
      </table>
    </div>
  )
}
