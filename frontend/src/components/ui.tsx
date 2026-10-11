import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

import { ApiError } from '../lib/api'
import { formatTnd } from '../lib/format'
import { useLang } from '../lib/hooks'
import type { OrderStatus } from '../lib/types'

export function Spinner({ label }: { label?: string }) {
  const { t } = useTranslation()
  return (
    <div className="flex items-center justify-center gap-3 py-16 text-gray-500" role="status">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-gray-300 border-t-ink" />
      <span>{label ?? t('common.loading')}</span>
    </div>
  )
}

export function ErrorBox({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const { t } = useTranslation()
  const message = error instanceof ApiError ? error.message : t('common.error_generic')
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert">
      <p>{message}</p>
      {onRetry && (
        <button className="mt-2 font-semibold underline" onClick={onRetry}>
          {t('common.retry')}
        </button>
      )}
    </div>
  )
}

export function FormError({ error }: { error: unknown }) {
  if (!error) return null
  const message = error instanceof ApiError ? error.message : String(error)
  const fields = error instanceof ApiError && error.fields ? Object.values(error.fields) : []
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800" role="alert">
      {message}
      {fields.length > 0 && (
        <ul className="mt-1 list-disc ps-5">
          {fields.map((f) => (
            <li key={f}>{f}</li>
          ))}
        </ul>
      )}
    </div>
  )
}

export function Notice({ children, tone = 'info' }: { children: ReactNode; tone?: 'info' | 'success' | 'warning' }) {
  const styles = {
    info: 'border-sky-200 bg-sky-50 text-sky-900',
    success: 'border-emerald-200 bg-emerald-50 text-emerald-900',
    warning: 'border-amber-200 bg-amber-50 text-amber-900',
  }[tone]
  return <div className={`rounded-lg border px-3 py-2 text-sm ${styles}`}>{children}</div>
}

export function Price({ value, className = '' }: { value: number | undefined | null; className?: string }) {
  const lang = useLang()
  return <span className={`ltr-nums whitespace-nowrap ${className}`}>{formatTnd(value, lang)}</span>
}

const STATUS_STYLES: Record<OrderStatus, string> = {
  PENDING_CONFIRMATION: 'bg-amber-100 text-amber-900',
  CONFIRMED: 'bg-sky-100 text-sky-900',
  PROCESSING: 'bg-indigo-100 text-indigo-900',
  SHIPPED: 'bg-violet-100 text-violet-900',
  DELIVERED: 'bg-emerald-100 text-emerald-900',
  DELIVERY_FAILED: 'bg-red-100 text-red-900',
  RETURNED: 'bg-gray-200 text-gray-800',
  CANCELLED: 'bg-gray-200 text-gray-700',
}

export function StatusBadge({ status }: { status: OrderStatus }) {
  const { t } = useTranslation()
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${STATUS_STYLES[status]}`}>
      {t(`status.${status}`)}
    </span>
  )
}

const PALETTE = ['#dbeafe', '#fce7f3', '#dcfce7', '#fef3c7', '#ede9fe', '#ffedd5', '#e0f2fe', '#f1f5f9']

/** Image produit, ou vignette colorée avec les initiales si aucune image n'est fournie. */
export function ProductImage({ src, title, className = '' }: { src?: string; title: string; className?: string }) {
  if (src) {
    return <img src={src} alt={title} loading="lazy" className={`object-contain ${className}`} />
  }
  const hash = [...title].reduce((acc, c) => acc + c.charCodeAt(0), 0)
  const initials = title
    .split(/\s+/)
    .filter((w) => /^[\p{L}\d]/u.test(w))
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join('')
  return (
    <div
      className={`flex items-center justify-center font-bold text-ink/70 ${className}`}
      style={{ background: PALETTE[hash % PALETTE.length] }}
      aria-label={title}
      role="img"
    >
      <span className="text-3xl">{initials}</span>
    </div>
  )
}

export function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="label">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-xs text-gray-500">{hint}</span>}
    </label>
  )
}
