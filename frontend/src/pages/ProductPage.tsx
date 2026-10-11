import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router-dom'

import { useAuth } from '../auth/context'
import { ErrorBox, FormError, Notice, Price, ProductImage, Spinner } from '../components/ui'
import { api, query } from '../lib/api'
import { formatDate, localName } from '../lib/format'
import { useGovernorates, useLang } from '../lib/hooks'
import type { Address, Cart, Offer, ProductDetail } from '../lib/types'

function OfferDelivery({ offer }: { offer: Offer }) {
  const { t } = useTranslation()
  const lang = useLang()
  return (
    <p className="text-sm text-gray-700">
      🚚 {t('product.delivery_estimate', { from: formatDate(offer.deliveryFrom, lang), to: formatDate(offer.deliveryTo, lang) })}
    </p>
  )
}

export default function ProductPage() {
  const { id } = useParams()
  const { t } = useTranslation()
  const lang = useLang()
  const { user } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { data: governorates } = useGovernorates()
  const addresses = useQuery({ queryKey: ['addresses'], queryFn: () => api.get<Address[]>('/me/addresses'), enabled: !!user })
  const [chosenGovernorate, setChosenGovernorate] = useState<string | null>(null)
  const governorate = chosenGovernorate ?? (addresses.data?.[0] ? String(addresses.data[0].governorateId) : '')
  const [quantity, setQuantity] = useState(1)
  const [added, setAdded] = useState(false)

  const product = useQuery({
    queryKey: ['product', id, governorate],
    queryFn: () => api.get<ProductDetail>(`/products/${id}${query({ governorate })}`),
  })

  const addToCart = useMutation({
    mutationFn: (offerId: number) => api.post<Cart>('/cart/items', { offerId, quantity }),
    onSuccess: (cart) => {
      queryClient.setQueryData(['cart'], cart)
      setAdded(true)
    },
  })

  const buy = (offerId: number, thenCheckout: boolean) => {
    if (!user) {
      navigate(`/login?next=/products/${id}`)
      return
    }
    addToCart.mutate(offerId, { onSuccess: () => thenCheckout && navigate('/checkout') })
  }

  if (product.isLoading) return <Spinner />
  if (product.error || !product.data) return <ErrorBox error={product.error} onRetry={() => product.refetch()} />
  const p = product.data
  const offer = p.buyBox
  const discount = offer?.compareAtPrice ? Math.round((1 - offer.price / offer.compareAtPrice) * 100) : 0

  return (
    <div className="space-y-6">
      <nav className="flex flex-wrap gap-1 text-sm text-gray-600" aria-label="breadcrumb">
        {p.breadcrumb.map((c, i) => (
          <span key={c.id}>
            {i > 0 && <span className="mx-1">›</span>}
            <Link to={`/search?category=${c.id}`} className="hover:underline">{localName(c, lang)}</Link>
          </span>
        ))}
      </nav>

      <div className="grid gap-6 lg:grid-cols-[1fr_1.1fr_320px]">
        <div className="card h-fit overflow-hidden">
          <ProductImage src={p.imageUrl} title={p.title} className="aspect-square w-full" />
        </div>

        <div className="space-y-4">
          {p.brand && (
            <Link to={`/search?brand=${p.brand.id}`} className="text-sm font-semibold text-ink-soft hover:underline">
              {p.brand.name}
            </Link>
          )}
          <h1 className="text-2xl font-bold text-gray-900">{p.title}</h1>
          {offer && (
            <div className="flex flex-wrap items-baseline gap-3">
              <Price value={offer.price} className="text-3xl font-extrabold text-gray-900" />
              <span className="text-xs text-gray-500">{t('product.vat_included')}</span>
              {offer.compareAtPrice && (
                <>
                  <Price value={offer.compareAtPrice} className="text-gray-500 line-through" />
                  <span className="rounded bg-red-600 px-2 py-0.5 text-xs font-bold text-white">{t('product.save_pct', { pct: discount })}</span>
                </>
              )}
            </div>
          )}
          <div>
            <h2 className="mb-1 font-semibold text-gray-900">{t('product.description')}</h2>
            <p className="whitespace-pre-line text-gray-700">{p.description}</p>
          </div>
          <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-1 text-sm">
            {p.brand && (
              <>
                <dt className="text-gray-500">{t('product.brand')}</dt>
                <dd>{p.brand.name}</dd>
              </>
            )}
            {p.ean && (
              <>
                <dt className="text-gray-500">{t('product.ean')}</dt>
                <dd className="ltr-nums">{p.ean}</dd>
              </>
            )}
            <dt className="text-gray-500">{t('product.vat')}</dt>
            <dd className="ltr-nums">{p.vatBp / 100} %</dd>
          </dl>
        </div>

        <aside className="card h-fit space-y-4 p-4">
          {offer ? (
            <>
              <Price value={offer.price} className="text-2xl font-bold" />
              <label className="block text-sm">
                <span className="text-gray-600">{t('product.deliver_to')}</span>
                <select className="input mt-1" value={governorate} onChange={(e) => setChosenGovernorate(e.target.value)}>
                  <option value="">{t('account.choose')}</option>
                  {governorates?.map((g) => (
                    <option key={g.id} value={g.id}>{localName(g, lang)}</option>
                  ))}
                </select>
              </label>
              <OfferDelivery offer={offer} />
              <p className="font-semibold text-emerald-700">{t('product.in_stock')}</p>
              <label className="flex items-center gap-2 text-sm">
                {t('product.quantity')}
                <select className="input w-20" value={quantity} onChange={(e) => setQuantity(Number(e.target.value))}>
                  {Array.from({ length: offer.maxQuantity }, (_, i) => i + 1).map((n) => (
                    <option key={n}>{n}</option>
                  ))}
                </select>
              </label>
              <div className="space-y-2">
                <button className="btn btn-primary w-full" onClick={() => buy(offer.id, false)} disabled={addToCart.isPending}>
                  {t('product.add_to_cart')}
                </button>
                <button className="btn btn-dark w-full" onClick={() => buy(offer.id, true)} disabled={addToCart.isPending}>
                  {t('product.buy_now')}
                </button>
              </div>
              {added && (
                <Notice tone="success">
                  {t('product.added_to_cart')} · <Link to="/cart" className="font-semibold underline">{t('common.cart')}</Link>
                </Notice>
              )}
              <FormError error={addToCart.error} />
              <div className="space-y-1 border-t border-gray-100 pt-3 text-sm text-gray-600">
                <p>
                  {t('product.sold_by')} <span className="font-semibold text-gray-900">{offer.shopName}</span>
                  {offer.sellerRating && <span className="ms-1">★ {offer.sellerRating}</span>}
                </p>
                <p>💵 {t('product.cod_available')}</p>
                <p>↩️ {t('product.returns_info')}</p>
              </div>
            </>
          ) : (
            <p className="font-semibold text-red-700">{t('product.unavailable')}</p>
          )}
        </aside>
      </div>

      {p.otherOffers.length > 0 && (
        <section className="card p-4">
          <h2 className="mb-3 font-bold text-gray-900">{t('product.other_sellers')}</h2>
          <ul className="divide-y divide-gray-100">
            {p.otherOffers.map((o) => (
              <li key={o.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                <div>
                  <Price value={o.price} className="text-lg font-bold" />
                  <p className="text-sm text-gray-600">
                    {t('product.sold_by')} <span className="font-semibold">{o.shopName}</span>
                  </p>
                  {o.inStock ? <OfferDelivery offer={o} /> : <p className="text-sm text-red-700">{t('product.out_of_stock')}</p>}
                </div>
                {o.inStock && (
                  <button className="btn btn-ghost" onClick={() => buy(o.id, false)}>
                    {t('product.add_to_cart')}
                  </button>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}
