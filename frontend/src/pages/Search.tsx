import { useQuery } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'

import ProductCardView from '../components/ProductCardView'
import { ErrorBox, Spinner } from '../components/ui'
import { api, query } from '../lib/api'
import { localName, parseTnd } from '../lib/format'
import { flattenCategories, useCategories, useLang } from '../lib/hooks'
import type { Brand, Page, ProductCard } from '../lib/types'

export default function Search() {
  const { t } = useTranslation()
  const lang = useLang()
  const [params, setParams] = useSearchParams()
  const { data: categories } = useCategories()
  const brands = useQuery({ queryKey: ['brands'], queryFn: () => api.get<Brand[]>('/brands'), staleTime: 600_000 })

  const q = params.get('q') ?? ''
  const page = Number(params.get('page') ?? 0)
  const [minPrice, setMinPrice] = useState(params.get('min') ?? '')
  const [maxPrice, setMaxPrice] = useState(params.get('max') ?? '')

  const results = useQuery({
    queryKey: ['products', params.toString()],
    queryFn: () =>
      api.get<Page<ProductCard>>(
        `/products${query({
          q,
          category: params.get('category'),
          brand: params.get('brand'),
          minPrice: params.get('min') ? parseTnd(params.get('min')!) : undefined,
          maxPrice: params.get('max') ? parseTnd(params.get('max')!) : undefined,
          inStock: params.get('inStock') === 'true' || undefined,
          sort: params.get('sort'),
          page,
          size: 24,
        })}`,
      ),
  })

  const update = (changes: Record<string, string | null>) => {
    const next = new URLSearchParams(params)
    Object.entries(changes).forEach(([k, v]) => (v ? next.set(k, v) : next.delete(k)))
    if (!('page' in changes)) next.delete('page')
    setParams(next)
  }

  const applyPrice = (e: FormEvent) => {
    e.preventDefault()
    update({ min: minPrice || null, max: maxPrice || null })
  }

  const flat = categories ? flattenCategories(categories) : []
  const currentCategory = flat.find((c) => String(c.node.id) === params.get('category'))?.node
  const heading = q ? t('search.results_for', { q }) : currentCategory ? localName(currentCategory, lang) : t('search.all_products')

  return (
    <div className="grid gap-6 lg:grid-cols-[240px_1fr]">
      <aside className="card h-fit space-y-5 p-4 text-sm">
        <h2 className="font-bold text-gray-900">{t('search.filters')}</h2>
        <div>
          <label className="label" htmlFor="f-category">{t('search.category')}</label>
          <select id="f-category" className="input" value={params.get('category') ?? ''} onChange={(e) => update({ category: e.target.value || null })}>
            <option value="">{t('common.all_categories')}</option>
            {flat.map(({ node, depth }) => (
              <option key={node.id} value={node.id}>
                {'  '.repeat(depth)}
                {localName(node, lang)}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="label" htmlFor="f-brand">{t('search.brand')}</label>
          <select id="f-brand" className="input" value={params.get('brand') ?? ''} onChange={(e) => update({ brand: e.target.value || null })}>
            <option value="">{t('search.any_brand')}</option>
            {brands.data?.map((b) => (
              <option key={b.id} value={b.id}>{b.name}</option>
            ))}
          </select>
        </div>
        <form onSubmit={applyPrice}>
          <span className="label">{t('search.price')}</span>
          <div className="flex gap-2">
            <input className="input ltr-nums" placeholder={t('search.price_min')} value={minPrice} onChange={(e) => setMinPrice(e.target.value)} inputMode="decimal" aria-label={t('search.price_min')} />
            <input className="input ltr-nums" placeholder={t('search.price_max')} value={maxPrice} onChange={(e) => setMaxPrice(e.target.value)} inputMode="decimal" aria-label={t('search.price_max')} />
          </div>
          <button className="btn btn-ghost mt-2 w-full">{t('search.apply')}</button>
        </form>
        <label className="flex items-center gap-2">
          <input type="checkbox" checked={params.get('inStock') === 'true'} onChange={(e) => update({ inStock: e.target.checked ? 'true' : null })} />
          {t('search.in_stock_only')}
        </label>
        <button
          className="text-sm font-semibold text-ink-soft hover:underline"
          onClick={() => {
            setMinPrice('')
            setMaxPrice('')
            setParams(q ? { q } : {})
          }}
        >
          {t('search.reset')}
        </button>
      </aside>

      <section>
        <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 className="text-xl font-bold text-gray-900">{heading}</h1>
            {results.data && <p className="text-sm text-gray-600">{t('search.results_count', { count: results.data.totalItems })}</p>}
          </div>
          <label className="flex items-center gap-2 text-sm">
            {t('search.sort')}
            <select className="input w-auto" value={params.get('sort') ?? ''} onChange={(e) => update({ sort: e.target.value || null })}>
              <option value="">{t('search.sort_relevance')}</option>
              <option value="price_asc">{t('search.sort_price_asc')}</option>
              <option value="price_desc">{t('search.sort_price_desc')}</option>
            </select>
          </label>
        </div>

        {results.isLoading && <Spinner />}
        {results.error && <ErrorBox error={results.error} onRetry={() => results.refetch()} />}
        {results.data?.items.length === 0 && (
          <div className="card p-10 text-center">
            <p className="font-semibold text-gray-900">{t('search.no_results')}</p>
            <p className="mt-1 text-sm text-gray-600">{t('search.no_results_hint')}</p>
          </div>
        )}
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 xl:grid-cols-4">
          {results.data?.items.map((p) => <ProductCardView key={p.id} product={p} />)}
        </div>

        {results.data && results.data.totalPages > 1 && (
          <nav className="mt-6 flex items-center justify-center gap-3 text-sm">
            <button className="btn btn-ghost" disabled={page === 0} onClick={() => update({ page: String(page - 1) })}>
              {t('search.prev')}
            </button>
            <span>{t('search.page_x_of_y', { page: page + 1, total: results.data.totalPages })}</span>
            <button className="btn btn-ghost" disabled={page + 1 >= results.data.totalPages} onClick={() => update({ page: String(page + 1) })}>
              {t('search.next')}
            </button>
          </nav>
        )}
      </section>
    </div>
  )
}
