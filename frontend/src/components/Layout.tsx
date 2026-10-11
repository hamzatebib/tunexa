import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, NavLink, Outlet, useNavigate, useSearchParams } from 'react-router-dom'

import { useAuth } from '../auth/context'
import { localName } from '../lib/format'
import { useCart, useCategories, useLang } from '../lib/hooks'
import type { Lang } from '../lib/types'

const CURRENT_YEAR = new Date().getFullYear()

const LANGS: { code: Lang; label: string }[] = [
  { code: 'fr', label: 'FR' },
  { code: 'ar', label: 'ع' },
  { code: 'en', label: 'EN' },
]

function Logo() {
  return (
    <Link to="/" className="flex items-center gap-2 text-xl font-extrabold tracking-tight text-white">
      <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent text-lg text-ink">T</span>
      <span>
        tunexa<span className="text-accent">.tn</span>
      </span>
    </Link>
  )
}

function SearchBar() {
  const { t } = useTranslation()
  const lang = useLang()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { data: categories } = useCategories()
  const [q, setQ] = useState(params.get('q') ?? '')
  const [category, setCategory] = useState(params.get('category') ?? '')

  const submit = (e: FormEvent) => {
    e.preventDefault()
    const search = new URLSearchParams()
    if (q.trim()) search.set('q', q.trim())
    if (category) search.set('category', category)
    navigate(`/search?${search.toString()}`)
  }

  return (
    <form onSubmit={submit} className="flex w-full overflow-hidden rounded-lg bg-white" role="search">
      <select
        value={category}
        onChange={(e) => setCategory(e.target.value)}
        className="hidden max-w-44 border-e border-gray-200 bg-gray-100 px-2 text-sm text-gray-700 outline-none sm:block"
        aria-label={t('search.category')}
      >
        <option value="">{t('common.all_categories')}</option>
        {categories?.map((c) => (
          <option key={c.id} value={c.id}>
            {localName(c, lang)}
          </option>
        ))}
      </select>
      <input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder={t('common.search_placeholder')}
        className="min-w-0 flex-1 px-3 py-2.5 text-sm text-gray-900 outline-none"
        aria-label={t('common.search')}
      />
      <button type="submit" className="bg-accent px-4 text-ink hover:bg-accent-dark" aria-label={t('common.search')}>
        <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2.5">
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-3.5-3.5" />
        </svg>
      </button>
    </form>
  )
}

function Header() {
  const { t, i18n } = useTranslation()
  const lang = useLang()
  const { user, signOut } = useAuth()
  const { data: cart } = useCart()
  const { data: categories } = useCategories()
  const navigate = useNavigate()

  return (
    <header className="bg-ink text-white">
      <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-x-6 gap-y-3 px-4 py-3">
        <Logo />
        <div className="order-last w-full md:order-none md:w-auto md:flex-1">
          <SearchBar />
        </div>
        <div className="ms-auto flex items-center gap-4 text-sm">
          <div className="flex rounded-md bg-white/10 p-0.5" aria-label={t('common.language')}>
            {LANGS.map((l) => (
              <button
                key={l.code}
                onClick={() => i18n.changeLanguage(l.code)}
                className={`rounded px-2 py-1 text-xs font-semibold ${lang === l.code ? 'bg-white text-ink' : 'text-white/80 hover:text-white'}`}
              >
                {l.label}
              </button>
            ))}
          </div>
          {user ? (
            <div className="group relative">
              <Link to="/account" className="block leading-tight">
                <span className="block text-xs text-white/70">{t('common.hello', { name: user.firstName })}</span>
                <span className="font-semibold">{t('common.account')}</span>
              </Link>
              <div className="invisible absolute end-0 z-20 mt-0 w-52 rounded-lg bg-white py-2 text-gray-800 opacity-0 shadow-lg transition group-hover:visible group-hover:opacity-100 group-focus-within:visible group-focus-within:opacity-100">
                <Link className="block px-4 py-2 hover:bg-gray-50" to="/account">{t('common.account')}</Link>
                <Link className="block px-4 py-2 hover:bg-gray-50" to="/orders">{t('common.orders')}</Link>
                <Link className="block px-4 py-2 hover:bg-gray-50" to="/seller">
                  {user.role === 'SELLER' ? t('common.seller_center') : t('common.sell_on_tunexa')}
                </Link>
                {user.role === 'ADMIN' && (
                  <Link className="block px-4 py-2 hover:bg-gray-50" to="/admin">{t('common.admin')}</Link>
                )}
                <button
                  className="block w-full px-4 py-2 text-start text-red-700 hover:bg-gray-50"
                  onClick={() => {
                    signOut()
                    navigate('/')
                  }}
                >
                  {t('common.logout')}
                </button>
              </div>
            </div>
          ) : (
            <Link to="/login" className="font-semibold hover:text-accent">
              {t('common.login')}
            </Link>
          )}
          <Link to="/cart" className="relative flex items-center gap-1 font-semibold hover:text-accent">
            <svg viewBox="0 0 24 24" className="h-7 w-7" fill="none" stroke="currentColor" strokeWidth="1.8">
              <path d="M3 4h2l2.4 11.2a2 2 0 0 0 2 1.6h7.7a2 2 0 0 0 2-1.5L21 8H6.2" />
              <circle cx="10" cy="20" r="1.3" />
              <circle cx="17" cy="20" r="1.3" />
            </svg>
            <span className="hidden sm:inline">{t('common.cart')}</span>
            {!!cart?.itemCount && (
              <span className="absolute -top-1.5 start-4 rounded-full bg-accent px-1.5 text-xs font-bold text-ink">
                {cart.itemCount}
              </span>
            )}
          </Link>
        </div>
      </div>
      <nav className="bg-ink-soft">
        <div className="mx-auto flex max-w-7xl gap-5 overflow-x-auto px-4 py-2 text-sm whitespace-nowrap">
          {categories?.map((c) => (
            <NavLink key={c.id} to={`/search?category=${c.id}`} className="text-white/85 hover:text-white">
              {localName(c, lang)}
            </NavLink>
          ))}
        </div>
      </nav>
    </header>
  )
}

function Footer() {
  const { t } = useTranslation()
  return (
    <footer className="mt-16 bg-ink text-sm text-white/75">
      <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:grid-cols-3">
        <div>
          <Logo />
          <p className="mt-3 max-w-xs">{t('common.footer_tagline')}</p>
        </div>
        <div>
          <h3 className="mb-2 font-semibold text-white">{t('common.footer_help')}</h3>
          <ul className="space-y-1">
            <li>{t('common.footer_help_cod')}</li>
            <li>{t('common.footer_help_delivery')}</li>
            <li>{t('common.footer_help_returns')}</li>
          </ul>
        </div>
        <div>
          <h3 className="mb-2 font-semibold text-white">{t('common.footer_company')}</h3>
          <ul className="space-y-1">
            <li>
              <Link to="/seller" className="hover:text-white">{t('common.sell_on_tunexa')}</Link>
            </li>
            <li>{t('common.footer_terms')}</li>
            <li>{t('common.footer_privacy')}</li>
          </ul>
        </div>
      </div>
      <div className="border-t border-white/10 py-4 text-center text-xs">
        {t('common.footer_rights', { year: CURRENT_YEAR })}
      </div>
    </footer>
  )
}

export default function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}
