import type { Lang } from './types'

const LOCALES: Record<Lang, string> = { fr: 'fr-FR', ar: 'ar-TN', en: 'en-GB' }

/** Formate un montant en millimes : 1249900 → « 1 249,900 DT ». */
export function formatTnd(millimes: number | undefined | null, lang: Lang = 'fr'): string {
  if (millimes === undefined || millimes === null) return '—'
  const value = (millimes / 1000).toLocaleString(lang === 'en' ? 'en-GB' : 'fr-FR', {
    minimumFractionDigits: 3,
    maximumFractionDigits: 3,
  })
  return lang === 'ar' ? `${value} د.ت` : `${value} DT`
}

/** Convertit une saisie en dinars (« 549,900 » ou « 549.9 ») en millimes. */
export function parseTnd(input: string): number | null {
  const normalized = input.replace(/\s/g, '').replace(',', '.')
  if (!/^\d+(\.\d{1,3})?$/.test(normalized)) return null
  return Math.round(Number(normalized) * 1000)
}

export function formatDate(iso: string, lang: Lang, withTime = false): string {
  const date = new Date(iso)
  return date.toLocaleDateString(LOCALES[lang], {
    day: 'numeric',
    month: 'long',
    year: withTime ? 'numeric' : undefined,
    weekday: withTime ? undefined : 'long',
    hour: withTime ? '2-digit' : undefined,
    minute: withTime ? '2-digit' : undefined,
  })
}

export function formatPhone(phone: string): string {
  const m = phone.match(/^\+216(\d{2})(\d{3})(\d{3})$/)
  return m ? `+216 ${m[1]} ${m[2]} ${m[3]}` : phone
}

export function localName(item: { nameFr: string; nameAr: string; nameEn: string }, lang: Lang): string {
  return lang === 'ar' ? item.nameAr : lang === 'en' ? item.nameEn : item.nameFr
}
