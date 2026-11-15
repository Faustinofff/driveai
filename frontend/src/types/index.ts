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
  version?: number
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
  userId: number
  items: CartItem[]
  subtotal: number
}

export interface CartAddRequest {
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
  userId: number
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
  GASOLINA:
    'bg-amber-100 text-amber-800 border-amber-200 dark:bg-slate-900/85 dark:text-amber-300 dark:border-amber-400/40',
  DIESEL:
    'bg-rose-100 text-rose-800 border-rose-200 dark:bg-slate-900/85 dark:text-rose-300 dark:border-rose-400/40',
  HIBRIDO:
    'bg-emerald-100 text-emerald-800 border-emerald-200 dark:bg-slate-900/85 dark:text-emerald-300 dark:border-emerald-400/40',
  ELECTRICO:
    'bg-sky-100 text-sky-800 border-sky-200 dark:bg-slate-900/85 dark:text-sky-300 dark:border-sky-400/40',
}

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: 'Pendiente',
  CONFIRMED: 'Confirmada',
  CANCELLED: 'Cancelada',
}

export const ORDER_STATUS_COLORS: Record<OrderStatus, string> = {
  PENDING:
    'bg-amber-100 text-amber-800 border-amber-200 dark:bg-amber-500/10 dark:text-amber-300 dark:border-amber-500/40',
  CONFIRMED:
    'bg-emerald-100 text-emerald-800 border-emerald-200 dark:bg-emerald-500/10 dark:text-emerald-300 dark:border-emerald-500/40',
  CANCELLED:
    'bg-slate-100 text-slate-600 border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700',
}

export type UserRole = 'ADMIN' | 'CUSTOMER'

export interface User {
  id: number
  email: string
  fullName: string
  role: UserRole
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  fullName: string
}

export interface AuthResponse {
  token: string
  user: User
}

export interface CarWriteRequest {
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