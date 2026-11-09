import { useState } from 'react'
import { Loader2, Minus, Plus, ShoppingCart, Trash2, X } from 'lucide-react'
import { checkout } from '../services/api'
import type { ApiError } from '../services/api'
import {
  selectLines,
  selectSubtotal,
  useCartStore,
} from '../store/cartStore'
import type { Order } from '../types'
import { formatMoney, formatNumber } from '../utils/formatters'

interface CartDrawerProps {
  open: boolean
  onClose: () => void
  onOrderCreated: (order: Order) => void
}

export function CartDrawer({ open, onClose, onOrderCreated }: CartDrawerProps) {
  const lines = useCartStore(selectLines)
  const subtotal = useCartStore(selectSubtotal)
  const increment = useCartStore((state) => state.increment)
  const decrement = useCartStore((state) => state.decrement)
  const remove = useCartStore((state) => state.remove)
  const clear = useCartStore((state) => state.clear)

  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!open) return null

  const handleCheckout = async () => {
    setSubmitting(true)
    setError(null)
    try {
      const order = await checkout({
        items: lines.map((line) => ({ carId: line.car.id, quantity: line.quantity })),
      })
      clear()
      onOrderCreated(order)
      onClose()
    } catch (err) {
      setError((err as ApiError).message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm" onClick={onClose} />

      <aside className="absolute right-0 top-0 flex h-full w-full max-w-md flex-col bg-white shadow-2xl animate-slide-in-right">
        <header className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <h2 className="flex items-center gap-2 text-lg font-bold">
            <ShoppingCart className="h-5 w-5 text-brand-600" />
            Mi carrito
            {lines.length > 0 && (
              <span className="rounded-full bg-brand-100 px-2 py-0.5 text-xs font-semibold text-brand-700">
                {lines.length}
              </span>
            )}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="flex h-8 w-8 items-center justify-center rounded-full text-slate-500 transition hover:bg-slate-100 hover:text-slate-700"
            aria-label="Cerrar carrito"
          >
            <X className="h-5 w-5" />
          </button>
        </header>

        {lines.length === 0 ? (
          <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
            <ShoppingCart className="h-12 w-12 text-slate-300" />
            <p className="font-semibold text-slate-700">Tu carrito está vacío</p>
            <p className="text-sm text-slate-500">
              Explora el catálogo o pídele recomendaciones al asesor de IA.
            </p>
          </div>
        ) : (
          <>
            <div className="flex-1 space-y-3 overflow-y-auto px-5 py-4 scrollbar-none">
              {lines.map((line) => (
                <div
                  key={line.car.id}
                  className="flex gap-3 rounded-2xl border border-slate-200 p-3"
                >
                  <img
                    src={line.car.imageUrl}
                    alt={line.car.model}
                    loading="lazy"
                    className="h-20 w-24 shrink-0 rounded-xl object-cover"
                  />
                  <div className="flex flex-1 flex-col">
                    <div className="flex items-start justify-between gap-2">
                      <p className="text-sm font-bold leading-tight text-slate-900">
                        {line.car.brand} {line.car.model}
                      </p>
                      <button
                        type="button"
                        onClick={() => remove(line.car.id)}
                        className="text-slate-400 transition hover:text-rose-600"
                        aria-label="Quitar del carrito"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                    <p className="text-xs text-slate-500">{line.car.year}</p>
                    <div className="mt-auto flex items-center justify-between pt-2">
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => decrement(line.car.id)}
                          className="flex h-7 w-7 items-center justify-center rounded-lg border border-slate-300 text-slate-600 transition hover:border-brand-400 disabled:opacity-40"
                          aria-label="Disminuir cantidad"
                        >
                          <Minus className="h-3.5 w-3.5" />
                        </button>
                        <span className="min-w-6 text-center text-sm font-bold">
                          {formatNumber(line.quantity)}
                        </span>
                        <button
                          type="button"
                          onClick={() => increment(line.car.id)}
                          disabled={line.quantity >= line.car.stock}
                          className="flex h-7 w-7 items-center justify-center rounded-lg border border-slate-300 text-slate-600 transition hover:border-brand-400 disabled:opacity-40"
                          aria-label="Aumentar cantidad"
                        >
                          <Plus className="h-3.5 w-3.5" />
                        </button>
                      </div>
                      <span className="text-sm font-extrabold text-brand-700">
                        {formatMoney(line.car.price * line.quantity)}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <footer className="space-y-3 border-t border-slate-200 px-5 py-4">
              <div className="flex items-center justify-between text-sm">
                <span className="text-slate-500">Subtotal</span>
                <span className="text-lg font-extrabold text-slate-900">{formatMoney(subtotal)}</span>
              </div>

              {error && (
                <p className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
                  {error}
                </p>
              )}

              <button
                type="button"
                disabled={submitting || lines.length === 0}
                onClick={() => void handleCheckout()}
                className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 py-3 text-sm font-bold text-white shadow-lg shadow-brand-600/25 transition hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
                {submitting ? 'Procesando pedido…' : 'Finalizar compra'}
              </button>
            </footer>
          </>
        )}
      </aside>
    </div>
  )
}