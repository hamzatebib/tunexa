import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'

import { useAuth } from '../auth/context'
import { api } from './api'
import type { Cart, CategoryNode, Governorate, Lang } from './types'

export function useLang(): Lang {
  const { i18n } = useTranslation()
  const lang = i18n.language
  return lang === 'ar' || lang === 'en' ? lang : 'fr'
}

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => api.get<CategoryNode[]>('/categories'),
    staleTime: 10 * 60_000,
  })
}

export function useGovernorates() {
  return useQuery({
    queryKey: ['governorates'],
    queryFn: () => api.get<Governorate[]>('/geo/governorates'),
    staleTime: Infinity,
  })
}

export function useCart() {
  const { user } = useAuth()
  return useQuery({
    queryKey: ['cart'],
    queryFn: () => api.get<Cart>('/cart'),
    enabled: !!user,
  })
}

/** Aplatit l'arbre des catégories (pour les listes déroulantes). */
export function flattenCategories(nodes: CategoryNode[], depth = 0): { node: CategoryNode; depth: number }[] {
  return nodes.flatMap((n) => [{ node: n, depth }, ...flattenCategories(n.children, depth + 1)])
}
