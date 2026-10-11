import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'

import { useAuth } from '../auth/context'
import { ErrorBox, FormError, Notice, Price, ProductImage, Spinner } from '../components/ui'
import { api } from '../lib/api'
import { formatTnd } from '../lib/format'
import { useCart, useLang } from '../lib/hooks'
import type { Cart } from '../lib/types'

const FREE_SHIPPING_THRESHOLD = 300_000

export default function CartPage() {
  const { t } = useTranslation()
  const lang = useLang()
  const { user } = useAuth()
  const cart = useCart()
  const queryClient = useQueryClient()

  const update = useMutation({
    mutationFn: ({ offerId, quantity }: { offerId: number; quantity: number }) =>
      quantity > 0
        ? api.put<Cart>(`/cart/items/${offerId}`, { quantity })
        : api.delete<Cart>(`/cart/items/${offerId}`),
    onSuccess: (data) => queryClient.setQueryData(['cart'], data),
  })

  if (!user) {
    return (
      <div className="card mx-auto max-w-md p-8 text-center">
        <p className="mb-4">{t('cart.empty')}</p>
        <Link to="/login?next=/cart" className="btn btn-primary">{t('common.login')}</Link>
      </div>
    )
  }
  if (cart.isLoading) return <Spinner />
  if (cart.error || !cart.data) return <ErrorBox error={cart.error} onRetry={() => cart.refetch()} />

  const data = cart.data
  if (data.groups.length === 0) {
    return (
      <div className="card mx-auto max-w-md p-8 text-center">
        <p className="mb-4 text-lg font-semibold">{t('cart.empty')}</p>
        <Link to="/search" className="btn btn-primary">{t('cart.empty_cta')}</Link>
      </div>
    )
  }

  const missing = FREE_SHIPPING_THRESHOLD - data.itemsTotal

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
      <section className="space-y-4">
        <h1 className="text-2xl font-bold text-gray-900">{t('cart.title')}</h1>
        {data.groups.map((group) => (
          <div key={group.sellerId} className="card">
            <h2 className="border-b border-gray-100 px-4 py-2 text-sm font-semibold text-gray-600">
              {t('cart.package_from', { shop: group.shopName })}
            </h2>
            <ul className="divide-y divide-gray-100">
              {group.lines.map((line) => (
                <li key={line.offerId} className="flex gap-4 p-4">
                  <Link to={`/products/${line.productId}`} className="w-24 shrink-0">
                    <ProductImage src={line.imageUrl} title={line.title} className="aspect-square w-24 rounded-lg" />
                  </Link>
                  <div className="flex flex-1 flex-col gap-2">
                    <Link to={`/products/${line.productId}`} className="font-medium text-gray-900 hover:underline">
                      {line.title}
                    </Link>
                    {!line.available && <p className="text-sm font-medium text-red-700">{t('cart.unavailable_line')}</p>}
                    <div className="mt-auto flex flex-wrap items-center gap-3 text-sm">
                      <select
                        className="input w-20"
                        value={line.quantity}
                        aria-label="quantity"
                        onChange={(e) => update.mutate({ offerId: line.offerId, quantity: Number(e.target.value) })}
                      >
                        {Array.from({ length: Math.max(line.maxQuantity, line.quantity) }, (_, i) => i + 1).map((n) => (
                          <option key={n}>{n}</option>
                        ))}
                      </select>
                      <button className="text-red-700 hover:underline" onClick={() => update.mutate({ offerId: line.offerId, quantity: 0 })}>
                        {t('cart.remove')}
                      </button>
                    </div>
                  </div>
                  <Price value={line.lineTotal} className="font-bold" />
                </li>
              ))}
            </ul>
          </div>
        ))}
        <FormError error={update.error} />
      </section>

      <aside className="card h-fit space-y-4 p-4">
        {missing > 0 ? (
          <Notice>{t('cart.free_shipping_left', { amount: formatTnd(missing, lang) })}</Notice>
        ) : (
          <Notice tone="success">{t('cart.free_shipping_ok')}</Notice>
        )}
        <p className="flex justify-between text-lg">
          <span>{t('cart.subtotal', { count: data.itemCount })}</span>
          <Price value={data.itemsTotal} className="font-bold" />
        </p>
        <Link
          to="/checkout"
          className={`btn btn-primary w-full ${data.checkoutAllowed ? '' : 'pointer-events-none opacity-50'}`}
          aria-disabled={!data.checkoutAllowed}
        >
          {t('cart.checkout')}
        </Link>
        <Link to="/search" className="block text-center text-sm text-ink-soft hover:underline">
          {t('cart.continue_shopping')}
        </Link>
      </aside>
    </div>
  )
}
