import axios, { AxiosError } from 'axios'
import type {
  AiRecommendRequest,
  AiRecommendResponse,
  AuthResponse,
  Car,
  CarFilters,
  CarWriteRequest,
  Cart,
  CartAddRequest,
  CartUpdateRequest,
  CheckoutRequest,
  LoginRequest,
  Order,
  PageResponse,
  RegisterRequest,
} from '../types'

const rawApiUrl = import.meta.env.VITE_API_URL as string | undefined

/**
 * VITE_API_URL acepta tanto el origen del backend (http://localhost:8080)
 * como la URL que ya incluye el prefijo /api (http://localhost:8080/api).
 * Normalizamos para que los endpoints se concatenen siempre igual y evitar
 * llamadas a /cars en lugar de /api/cars cuando se configura el origen.
 */
function resolveApiBaseUrl(raw?: string): string {
  if (!raw) return '/api'
  const trimmed = raw.replace(/\/+$/, '')
  return trimmed.endsWith('/api') ? trimmed : `${trimmed}/api`
}

const API_BASE_URL = resolveApiBaseUrl(rawApiUrl)

export const api = axios.create({
  baseURL: API_BASE_URL ?? '',
  timeout: 45_000,
  headers: { 'Content-Type': 'application/json' },
})

const AUTH_TOKEN_KEY = 'driveai-token'
let authToken: string | null =
  typeof localStorage !== 'undefined' ? localStorage.getItem(AUTH_TOKEN_KEY) : null

export function setAuthToken(token: string | null): void {
  authToken = token
  if (token) {
    localStorage.setItem(AUTH_TOKEN_KEY, token)
  } else {
    localStorage.removeItem(AUTH_TOKEN_KEY)
  }
}

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

api.interceptors.request.use((config) => {
  if (authToken) {
    config.headers.Authorization = `Bearer ${authToken}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const isAuthCall = error.config?.url?.includes('/auth/') ?? false
    if (error.response?.status === 401 && !isAuthCall) {
      setAuthToken(null)
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new Event('driveai:unauthorized'))
      }
    }
    return Promise.reject(normalizeError(error))
  },
)

// ───────────────────────── Autenticación ─────────────────────────

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/login', request)
  return data
}

export async function register(request: RegisterRequest): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/register', request)
  return data
}

// ───────────────────────── Administración (admin) ─────────────────────────

export async function createCar(request: CarWriteRequest): Promise<Car> {
  const { data } = await api.post<Car>('/cars', request)
  return data
}

export async function updateCar(id: number, request: CarWriteRequest, version?: number): Promise<Car> {
  const { data } = await api.put<Car>(`/cars/${id}`, request, {
    headers: version !== undefined ? { 'If-Match': `"${version}"` } : undefined,
  })
  return data
}

export async function deleteCar(id: number): Promise<void> {
  await api.delete(`/cars/${id}`)
}

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

export async function getCart(): Promise<Cart> {
  const { data } = await api.get<Cart>('/cart')
  return data
}

export async function addToCart(request: CartAddRequest): Promise<Cart> {
  const { data } = await api.post<Cart>('/cart/add', request)
  return data
}

export async function updateCartItem(itemId: number, request: CartUpdateRequest): Promise<Cart> {
  const { data } = await api.put<Cart>(`/cart/update/${itemId}`, request)
  return data
}

export async function removeCartItem(itemId: number): Promise<Cart> {
  const { data } = await api.delete<Cart>(`/cart/remove/${itemId}`)
  return data
}

// ───────────────────────── Órdenes ─────────────────────────

function newIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `idem-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

export async function checkout(
  request: CheckoutRequest,
  idempotencyKey: string = newIdempotencyKey(),
): Promise<Order> {
  const { data } = await api.post<Order>('/orders/checkout', request, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
  return data
}

export async function listOrders(): Promise<Order[]> {
  const { data } = await api.get<Order[]>('/orders')
  return data
}

export async function cancelOrder(id: number): Promise<Order> {
  const { data } = await api.post<Order>(`/orders/${id}/cancel`)
  return data
}

// ───────────────────────── Asesor IA ─────────────────────────

export async function recommendCars(request: AiRecommendRequest): Promise<AiRecommendResponse> {
  const { data } = await api.post<AiRecommendResponse>('/ai/recommend', request)
  return data
}