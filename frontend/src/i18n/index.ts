import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

import type { Lang } from '../lib/types'
import ar from './ar'
import en from './en'
import fr from './fr'

const LANG_KEY = 'tunexa.lang'

function initialLanguage(): Lang {
  try {
    const saved = localStorage.getItem(LANG_KEY)
    if (saved === 'fr' || saved === 'ar' || saved === 'en') return saved
  } catch {
    // stockage indisponible
  }
  return navigator.language?.startsWith('ar') ? 'ar' : navigator.language?.startsWith('en') ? 'en' : 'fr'
}

function applyDirection(lang: string) {
  document.documentElement.lang = lang
  document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr'
}

i18n.use(initReactI18next).init({
  resources: { fr: { translation: fr }, ar: { translation: ar }, en: { translation: en } },
  lng: initialLanguage(),
  fallbackLng: 'fr',
  interpolation: { escapeValue: false },
})

applyDirection(i18n.language)
i18n.on('languageChanged', (lang) => {
  applyDirection(lang)
  try {
    localStorage.setItem(LANG_KEY, lang)
  } catch {
    // stockage indisponible
  }
})

export default i18n
