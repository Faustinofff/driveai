export type FuelType = 'GASOLINA' | 'DIESEL' | 'HIBRIDO' | 'ELECTRICO'
export type Transmission = 'MANUAL' | 'AUTOMATICA'
export type BodyType = 'HATCHBACK' | 'SEDAN' | 'SUV' | 'PICKUP'
export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED'

export interface Car {
  id: number
  brand: string
  model: string
  year: number
  price: number
  mileage: number
  fuelType: FuelType
  fuelConsumption: number
  transmission: Transmission
  bodyType: BodyType
  imageUrl: string
  description: string
  stock: number
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface CartItem {
  id: number
  car: Car
  quantity: number
  priceSnapshot: number
  lineTotal: number
}

export interface Cart {
  sessionId: string
  items: CartItem[]
  subtotal: number
}

export interface CartAddRequest {
  sessionId: string
  carId: number
  quantity: number
}

export interface CartUpdateRequest {
  quantity: number
}

export interface CheckoutItem {
  carId: number
  quantity: number
}

export interface CheckoutRequest {
  sessionId: string
  items: CheckoutItem[]
}

export interface OrderItem {
  id: number
  car: Car
  quantity: number
  unitPrice: number
  lineTotal: number
}

export interface Order {
  id: number
  sessionId: string
  status: OrderStatus
  totalAmount: number
  items: OrderItem[]
  createdAt: string
}

export interface AiRecommendRequest {
  prompt: string
  sessionId: string
}

export interface AiRecommendResponse {
  sessionId: string
  reasoning: string | null
  recommendations: Car[]
}

export interface CarFilters {
  brand?: string
  minPrice?: number
  maxPrice?: number
  minYear?: number
  fuelType?: FuelType
  bodyType?: BodyType
  q?: string
}

export const FUEL_LABELS: Record<FuelType, string> = {
  GASOLINA: 'Gasolina',
  DIESEL: 'Diésel',
  HIBRIDO: 'Híbrido',
  ELECTRICO: 'Eléctrico',
}

export const BODY_LABELS: Record<BodyType, string> = {
  HATCHBACK: 'Hatchback',
  SEDAN: 'Sedán',
  SUV: 'SUV',
  PICKUP: 'Pickup',
}

export const TRANSMISSION_LABELS: Record<Transmission, string> = {
  MANUAL: 'Manual',
  AUTOMATICA: 'Automática',
}

export const FUEL_COLORS: Record<FuelType, string> = {
  GASOLINA: 'bg-amber-100 text-amber-800 border-amber-200',
  DIESEL: 'bg-rose-100 text-rose-800 border-rose-200',
  HIBRIDO: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  ELECTRICO: 'bg-sky-100 text-sky-800 border-sky-200',
}