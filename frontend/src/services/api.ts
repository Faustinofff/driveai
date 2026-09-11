import axios, { AxiosError } from 'axios'
import type {
  AiRecommendRequest,
  AiRecommendResponse,
  Car,
  CarFilters,
  Cart,
  CartAddRequest,
  CartUpdateRequest,
  CheckoutRequest,
  Order,
  PageResponse,
} from '../types'

const API_BASE_URL = (
  import.meta.env.VITE_API_URL as string | undefined
)?.replace(/\/$/, '')

export const api = axios.create({
  baseURL: API_BASE_URL ?? '',
  timeout: 45_000,
  headers: { 'Content-Type': 'application/json' },
})

export interface ApiError {
  message: string
  status?: number
  detail?: unknown
}

export function normalizeError(error: unknown): ApiError {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as
      | { title?: string; detail?: string; [key: string]: unknown }
      | undefined
    const status = error.response?.status
    if (data?.detail) {
      return { message: data.detail, status, detail: data }
    }
    if (data?.title) {
      return { message: data.title, status, detail: data }
    }
    if (status === 401) {
      return { message: 'Sesión no autorizada.', status }
    }
    if (status && status >= 500) {
      return { message: 'El servidor no respondió correctamente. Inténtalo de nuevo.', status }
    }
    if (error.code === 'ECONNABORTED') {
      return { message: 'La solicitud tardó demasiado. Inténtalo de nuevo.', status }
    }
    return {
      message: 'No se pudo conectar con el servidor. Inténtalo de nuevo.',
      status,
    }
  }
  return { message: 'Algo salió mal. Inténtalo de nuevo.' }
}

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => Promise.reject(normalizeError(error)),
)

// ───────────────────────── Catálogo ─────────────────────────

export async function getCars(
  params: CarFilters & { page: number; size?: number },
): Promise<PageResponse<Car>> {
  const search = new URLSearchParams()
  search.set('page', String(params.page))
  search.set('size', String(params.size ?? 12))
  if (params.brand) search.set('brand', params.brand)
  if (params.minPrice !== undefined && params.minPrice !== null) {
    search.set('minPrice', String(params.minPrice))
  }
  if (params.maxPrice !== undefined && params.maxPrice !== null) {
    search.set('maxPrice', String(params.maxPrice))
  }
  if (params.minYear !== undefined && params.minYear !== null) {
    search.set('minYear', String(params.minYear))
  }
  if (params.fuelType) search.set('fuelType', params.fuelType)
  if (params.bodyType) search.set('bodyType', params.bodyType)
  if (params.q) search.set('q', params.q)

  const { data } = await api.get<PageResponse<Car>>(`/cars?${search.toString()}`)
  return data
}

export async function getCar(id: number): Promise<Car> {
  const { data } = await api.get<Car>(`/cars/${id}`)
  return data
}

// ───────────────────────── Carrito ─────────────────────────

export async function getCart(sessionId: string): Promise<Cart> {
  const { data } = await api.get<Cart>('/cart', { params: { sessionId } })
  return data
}

export async function addToCart(request: CartAddRequest): Promise<Cart> {
  const { data } = await api.post<Cart>('/cart/add', request)
  return data
}

export async function updateCartItem(itemId: number, sessionId: string, request: CartUpdateRequest): Promise<Cart> {
  const { data } = await api.put<Cart>(`/cart/update/${itemId}`, request, { params: { sessionId } })
  return data
}

export async function removeCartItem(itemId: number, sessionId: string): Promise<Cart> {
  const { data } = await api.delete<Cart>(`/cart/remove/${itemId}`, { params: { sessionId } })
  return data
}

// ───────────────────────── Órdenes ─────────────────────────

export async function checkout(request: CheckoutRequest): Promise<Order> {
  const { data } = await api.post<Order>('/orders/checkout', request)
  return data
}

// ───────────────────────── Asesor IA ─────────────────────────

export async function recommendCars(request: AiRecommendRequest): Promise<AiRecommendResponse> {
  const { data } = await api.post<AiRecommendResponse>('/ai/recommend', request)
  return data
}