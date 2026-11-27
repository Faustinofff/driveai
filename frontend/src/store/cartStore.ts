import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Car } from '../types'
import { secureRandomId } from '../utils/secureRandomId'

export interface CartLine {
  car: Car
  quantity: number
}

/** Resultado de agregar: `limit` si la línea ya está al máximo del stock. */
export type AddResult = 'added' | 'limit'

interface CartStoreState {
  sessionId: string
  lines: CartLine[]
  add: (car: Car) => AddResult
  increment: (carId: number) => void
  decrement: (carId: number) => void
  setQuantity: (carId: number, quantity: number) => void
  remove: (carId: number) => void
  clear: () => void
}

function generateSessionId(): string {
  return `session-${secureRandomId()}`
}

const DEFAULT_QUANTITY = 1

export const useCartStore = create<CartStoreState>()(
  persist(
    (set, get) => ({
      sessionId: generateSessionId(),
      lines: [],

      add: (car) => {
        const current = get().lines
        const existing = current.find((line) => line.car.id === car.id)
        if (existing) {
          // Se refresca la ficha del auto (precio/stock pueden haber cambiado
          // desde que se guardó la línea) y se respeta el tope de stock.
          if (existing.quantity >= car.stock) {
            set({
              lines: current.map((line) =>
                line.car.id === car.id ? { ...line, car, quantity: Math.min(line.quantity, car.stock) } : line,
              ),
            })
            return 'limit'
          }
          const nextQty = Math.min(existing.quantity + DEFAULT_QUANTITY, car.stock)
          set({
            lines: current.map((line) =>
              line.car.id === car.id ? { ...line, car, quantity: nextQty } : line,
            ),
          })
          return 'added'
        }
        set({ lines: [...current, { car, quantity: DEFAULT_QUANTITY }] })
        return 'added'
      },

      increment: (carId) => {
        set({
          lines: get().lines.map((line) =>
            line.car.id === carId
              ? { ...line, quantity: Math.min(line.quantity + 1, line.car.stock) }
              : line,
          ),
        })
      },

      decrement: (carId) => {
        set({
          lines: get().lines.map((line) =>
            line.car.id === carId
              ? { ...line, quantity: Math.max(1, line.quantity - 1) }
              : line,
          ),
        })
      },

      setQuantity: (carId, quantity) => {
        set({
          lines: get().lines.map((line) =>
            line.car.id === carId
              ? {
                  ...line,
                  quantity: Math.max(1, Math.min(quantity, line.car.stock)),
                }
              : line,
          ),
        })
      },

      remove: (carId) => {
        set({ lines: get().lines.filter((line) => line.car.id !== carId) })
      },

      clear: () => set({ lines: [] }),
    }),
    { name: 'driveai-cart' },
  ),
)

export const selectSessionId = (state: CartStoreState): string => state.sessionId
export const selectLines = (state: CartStoreState): CartLine[] => state.lines
export const selectCount = (state: CartStoreState): number =>
  state.lines.reduce((total, line) => total + line.quantity, 0)
export const selectSubtotal = (state: CartStoreState): number =>
  state.lines.reduce((total, line) => total + line.car.price * line.quantity, 0)
export const selectHasItems = (state: CartStoreState): boolean => state.lines.length > 0