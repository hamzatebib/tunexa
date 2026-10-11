// Types des réponses de l'API Tunexa. Les montants sont en millimes (1 DT = 1000).

export type Role = 'CUSTOMER' | 'SELLER' | 'ADMIN'
export type Lang = 'fr' | 'ar' | 'en'

export interface User {
  id: number
  phone: string
  email?: string
  firstName: string
  lastName: string
  language: Lang
  role: Role
}

export interface AuthResponse {
  token: string
  user: User
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface Governorate {
  id: number
  code: string
  nameFr: string
  nameAr: string
  nameEn: string
  shippingZone: number
}

export interface CategoryNode {
  id: number
  slug: string
  nameFr: string
  nameAr: string
  nameEn: string
  children: CategoryNode[]
}

export interface Brand {
  id: number
  name: string
}

export interface ProductCard {
  id: number
  title: string
  brand?: string
  imageUrl?: string
  price: number
  inStock: boolean
  categoryId: number
}

export interface Offer {
  id: number
  sellerId: number
  shopName: string
  sellerRating?: number
  price: number
  compareAtPrice?: number
  inStock: boolean
  maxQuantity: number
  handlingDays: number
  condition: string
  deliveryFrom: string
  deliveryTo: string
}

export interface ProductDetail {
  id: number
  title: string
  description: string
  brand?: Brand
  breadcrumb: Omit<CategoryNode, 'children'>[]
  imageUrl?: string
  ean?: string
  vatBp: number
  buyBox?: Offer
  otherOffers: Offer[]
}

export interface Address {
  id: number
  label: string
  fullName: string
  phone: string
  governorateId: number
  governorateName: string
  delegation: string
  locality?: string
  postalCode: string
  street: string
  landmark?: string
  defaultAddress: boolean
}

export interface CartLine {
  offerId: number
  productId: number
  title: string
  brand?: string
  imageUrl?: string
  sellerId: number
  shopName: string
  unitPrice: number
  quantity: number
  maxQuantity: number
  lineTotal: number
  available: boolean
}

export interface Cart {
  groups: { sellerId: number; shopName: string; lines: CartLine[]; subtotal: number }[]
  itemCount: number
  itemsTotal: number
  checkoutAllowed: boolean
}

export interface Quote {
  packages: {
    sellerId: number
    shopName: string
    itemsTotal: number
    shippingFee: number
    deliveryFrom: string
    deliveryTo: string
  }[]
  itemsTotal: number
  shippingTotal: number
  stampDuty: number
  codFee: number
  total: number
  vatIncluded: number
  codAllowed: boolean
  codMaxTotal: number
  freeShippingThreshold: number
}

export type OrderStatus =
  | 'PENDING_CONFIRMATION'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'DELIVERY_FAILED'
  | 'RETURNED'
  | 'CANCELLED'

export interface ShippingAddress {
  fullName: string
  phone: string
  governorate: string
  delegation: string
  locality?: string
  postalCode: string
  street: string
  landmark?: string
}

export interface OrderItem {
  productId: number
  offerId: number
  title: string
  unitPrice: number
  quantity: number
  lineTotal: number
}

export interface OrderDetail {
  number: string
  status: OrderStatus
  paymentMethod: 'COD'
  createdAt: string
  confirmedAt?: string
  itemsTotal: number
  shippingTotal: number
  stampDuty: number
  codFee: number
  total: number
  cancellable: boolean
  shippingAddress: ShippingAddress
  packages: {
    id: number
    shopName: string
    status: OrderStatus
    trackingNumber?: string
    itemsTotal: number
    shippingFee: number
    cancellable: boolean
    items: OrderItem[]
  }[]
  events: { status: OrderStatus; packageId?: number; note?: string; createdAt: string }[]
}

export interface OrderSummary {
  number: string
  status: OrderStatus
  createdAt: string
  total: number
  itemCount: number
  firstItemTitle?: string
  packageCount: number
}

export type SellerStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED'

export interface Seller {
  id: number
  shopName: string
  legalName: string
  taxId: string
  rneNumber: string
  governorateName: string
  status: SellerStatus
  rejectionReason?: string
  commissionBp: number
  createdAt: string
}

export type ProductStatus = 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED'

export interface SellerOffer {
  offerId: number
  productId: number
  title: string
  brand?: string
  imageUrl?: string
  productStatus: ProductStatus
  rejectionReason?: string
  price: number
  compareAtPrice?: number
  stock: number
  handlingDays: number
  active: boolean
}

export interface SellerPackage {
  id: number
  orderNumber: string
  status: OrderStatus
  orderedAt: string
  paymentMethod: 'COD'
  itemsTotal: number
  shippingFee: number
  commission: number
  netAmount: number
  amountToCollect: number
  trackingNumber?: string
  cancelReason?: string
  shipTo: ShippingAddress
  items: OrderItem[]
}

export interface AdminStats {
  ordersToday: number
  revenueToday: number
  ordersLast30Days: number
  revenueLast30Days: number
  pendingSellers: number
  pendingProducts: number
  packagesToShip: number
  customers: number
}

export interface PendingProduct {
  id: number
  title: string
  brand?: string
  category: string
  imageUrl?: string
  description: string
  ean?: string
  price?: number
  sellerShopName?: string
  createdAt: string
}
