import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'

import type { ProductCard } from '../lib/types'
import { Price, ProductImage } from './ui'

export default function ProductCardView({ product }: { product: ProductCard }) {
  const { t } = useTranslation()
  return (
    <Link
      to={`/products/${product.id}`}
      className="card group flex flex-col overflow-hidden transition hover:-translate-y-0.5 hover:shadow-md"
    >
      <ProductImage src={product.imageUrl} title={product.title} className="aspect-square w-full bg-white" />
      <div className="flex flex-1 flex-col gap-1 p-3">
        {product.brand && <span className="text-xs font-medium tracking-wide text-gray-500 uppercase">{product.brand}</span>}
        <h3 className="line-clamp-2 text-sm text-gray-900 group-hover:text-ink-soft">{product.title}</h3>
        <div className="mt-auto pt-2">
          <Price value={product.price} className="text-lg font-bold text-gray-900" />
          {!product.inStock && <p className="text-xs font-medium text-red-700">{t('product.out_of_stock')}</p>}
        </div>
      </div>
    </Link>
  )
}
