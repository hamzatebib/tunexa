import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'

import ProductCardView from '../components/ProductCardView'
import { ErrorBox, Spinner } from '../components/ui'
import { api } from '../lib/api'
import { localName } from '../lib/format'
import { useCategories, useLang } from '../lib/hooks'
import type { Page, ProductCard } from '../lib/types'

const CATEGORY_ICONS: Record<string, string> = {
  'high-tech': '📱',
  electromenager: '🧺',
  mode: '👗',
  beaute: '💄',
  maison: '🛋️',
  bebe: '🧸',
  sport: '⚽',
  epicerie: '🫒',
  scolaire: '📚',
  artisanat: '🏺',
}

export default function Home() {
  const { t } = useTranslation()
  const lang = useLang()
  const { data: categories } = useCategories()
  const latest = useQuery({
    queryKey: ['products', 'latest'],
    queryFn: () => api.get<Page<ProductCard>>('/products?size=12'),
  })

  const perks = [
    { icon: '💵', title: t('home.perk_cod'), desc: t('home.perk_cod_desc') },
    { icon: '🚚', title: t('home.perk_delivery'), desc: t('home.perk_delivery_desc') },
    { icon: '✅', title: t('home.perk_sellers'), desc: t('home.perk_sellers_desc') },
  ]

  return (
    <div className="space-y-10">
      <section className="overflow-hidden rounded-2xl bg-gradient-to-br from-ink to-ink-soft px-6 py-12 text-white sm:px-12">
        <h1 className="max-w-2xl text-3xl font-extrabold leading-tight sm:text-4xl">{t('home.hero_title')}</h1>
        <p className="mt-3 max-w-xl text-white/80">{t('home.hero_subtitle')}</p>
        <Link to="/search" className="btn btn-primary mt-6 px-6 py-3 text-base">
          {t('home.hero_cta')}
        </Link>
      </section>

      <section className="grid gap-4 sm:grid-cols-3">
        {perks.map((p) => (
          <div key={p.title} className="card flex items-start gap-3 p-4">
            <span className="text-2xl" aria-hidden>{p.icon}</span>
            <div>
              <h2 className="font-semibold text-gray-900">{p.title}</h2>
              <p className="text-sm text-gray-600">{p.desc}</p>
            </div>
          </div>
        ))}
      </section>

      <section>
        <h2 className="mb-4 text-xl font-bold text-gray-900">{t('home.categories')}</h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
          {categories?.map((c) => (
            <Link
              key={c.id}
              to={`/search?category=${c.id}`}
              className="card flex flex-col items-center gap-2 p-4 text-center text-sm font-medium hover:border-ink"
            >
              <span className="text-3xl" aria-hidden>{CATEGORY_ICONS[c.slug] ?? '🛍️'}</span>
              {localName(c, lang)}
            </Link>
          ))}
        </div>
      </section>

      <section>
        <div className="mb-4 flex items-end justify-between">
          <h2 className="text-xl font-bold text-gray-900">{t('home.latest')}</h2>
          <Link to="/search" className="text-sm font-semibold text-ink-soft hover:underline">
            {t('home.see_all')}
          </Link>
        </div>
        {latest.isLoading && <Spinner />}
        {latest.error && <ErrorBox error={latest.error} onRetry={() => latest.refetch()} />}
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
          {latest.data?.items.map((p) => <ProductCardView key={p.id} product={p} />)}
        </div>
      </section>

      <section className="card flex flex-col items-start justify-between gap-4 p-6 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-lg font-bold text-gray-900">{t('home.sell_title')}</h2>
          <p className="text-sm text-gray-600">{t('home.sell_desc')}</p>
        </div>
        <Link to="/seller" className="btn btn-dark">
          {t('common.sell_on_tunexa')}
        </Link>
      </section>
    </div>
  )
}
