import { useState } from 'react'
import { Loader2, Minus, Plus, ShoppingCart, Trash2, X } from 'lucide-react'
import { useModalBehavior } from '../hooks/useModalBehavior'
import { checkout } from '../services/api'
import type { ApiError } from '../services/api'
import {
  selectCount,
  selectLines,
  selectSubtotal,
  useCartStore,
} from '../store/cartStore'
import type { Order } from '../types'
import { formatMoney, formatNumber } from '../utils/formatters'
import { ALERT_ERROR, ICON_BUTTON } from '../utils/styles'
import { CarImage } from './CarImage'
import { Portal } from './Portal'

interface CartDrawerProps {
  open: boolean
  onClose: () => void
  onOrderCreated: (order: Order) => void
}

export function CartDrawer({ open, onClose, onOrderCreated }: CartDrawerProps) {
  if (!open) return null
  return <CartDrawerPanel onClose={onClose} onOrderCreated={onOrderCreated} />
}

function CartDrawerPanel({ onClose, onOrderCreated }: Omit<CartDrawerProps, 'open'>) {
  const lines = useCartStore(selectLines)
  const units = useCartStore(selectCount)
  const subtotal = useCartStore(selectSubtotal)
  const increment = useCartStore((state) => state.increment)
  const decrement = useCartStore((state) => state.decrement)
  const remove = useCartStore((state) => state.remove)
  const clear = useCartStore((state) => state.clear)

  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const panelRef = useModalBehavior<HTMLElement>(onClose)

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
    <Portal>
      <div className="fixed inset-0 z-50" role="dialog" aria-modal="true" aria-labelledby="cart-title">
        <button
          type="button"
          tabIndex={-1}
          aria-label="Fondo del carrito"
          onClick={onClose}
          className="absolute inset-0 cursor-default bg-slate-900/50 backdrop-blur-sm"
        />

        <aside
          ref={panelRef}
          tabIndex={-1}
          className="absolute right-0 top-0 flex h-full w-full max-w-md flex-col bg-white text-slate-900 shadow-2xl outline-none animate-slide-in-right dark:bg-slate-900 dark:text-slate-100"
        >
          <header className="flex items-center justify-between border-b border-slate-200 px-5 py-4 dark:border-slate-800">
            <h2 id="cart-title" className="flex items-center gap-2 text-lg font-bold">
              <ShoppingCart className="h-5 w-5 text-brand-600 dark:text-brand-300" />
              Mi carrito
              {units > 0 && (
                <span className="rounded-full bg-brand-100 px-2 py-0.5 text-xs font-semibold text-brand-700 dark:bg-brand-500/15 dark:text-brand-200">
                  {units} {units === 1 ? 'unidad' : 'unidades'}
                </span>
              )}
            </h2>
            <button
              type="button"
              onClick={onClose}
              className={ICON_BUTTON}
              aria-label="Cerrar carrito"
            >
              <X className="h-5 w-5" />
            </button>
          </header>

          {lines.length === 0 ? (
            <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
              <ShoppingCart className="h-12 w-12 text-slate-300 dark:text-slate-600" />
              <p className="font-semibold text-slate-700 dark:text-slate-200">Tu carrito está vacío</p>
              <p className="text-sm text-slate-500 dark:text-slate-400">
                Explora el catálogo o pídele recomendaciones al asesor de IA.
              </p>
            </div>
          ) : (
            <>
              <div className="flex-1 space-y-3 overflow-y-auto px-5 py-4 scrollbar-none">
                {lines.map((line) => {
                  const atLimit = line.quantity >= line.car.stock
                  return (
                    <div
                      key={line.car.id}
                      className="flex gap-3 rounded-2xl border border-slate-200 p-3 dark:border-slate-800"
                    >
                      <CarImage
                        src={line.car.imageUrl}
                        alt={line.car.model}
                        className="h-20 w-24 shrink-0 rounded-xl"
                        compact
                      />
                      <div className="flex min-w-0 flex-1 flex-col">
                        <div className="flex items-start justify-between gap-2">
                          <p className="text-sm font-bold leading-tight text-slate-900 dark:text-white">
                            {line.car.brand} {line.car.model}
                          </p>
                          <button
                            type="button"
                            onClick={() => remove(line.car.id)}
                            className="shrink-0 text-slate-400 transition hover:text-rose-600 dark:hover:text-rose-400"
                            aria-label="Quitar del carrito"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          {line.car.year} · {formatMoney(line.car.price)} c/u
                        </p>
                        {atLimit && (
                          <p className="mt-0.5 text-[11px] font-medium text-amber-600 dark:text-amber-400">
                            Máximo disponible: {formatNumber(line.car.stock)}
                          </p>
                        )}
                        <div className="mt-auto flex items-center justify-between pt-2">
                          <div className="flex items-center gap-2">
                            <button
                              type="button"
                              onClick={() => decrement(line.car.id)}
                              disabled={line.quantity <= 1}
                              className="flex h-7 w-7 items-center justify-center rounded-lg border border-slate-300 text-slate-600 transition hover:border-brand-400 disabled:opacity-40 dark:border-slate-700 dark:text-slate-300"
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
                              disabled={atLimit}
                              className="flex h-7 w-7 items-center justify-center rounded-lg border border-slate-300 text-slate-600 transition hover:border-brand-400 disabled:opacity-40 dark:border-slate-700 dark:text-slate-300"
                              aria-label="Aumentar cantidad"
                            >
                              <Plus className="h-3.5 w-3.5" />
                            </button>
                          </div>
                          <span className="text-sm font-extrabold text-brand-700 dark:text-brand-300">
                            {formatMoney(line.car.price * line.quantity)}
                          </span>
                        </div>
                      </div>
                    </div>
                  )
                })}
              </div>

              <footer className="space-y-3 border-t border-slate-200 px-5 py-4 dark:border-slate-800">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-slate-500 dark:text-slate-400">Subtotal</span>
                  <span className="text-lg font-extrabold text-slate-900 dark:text-white">
                    {formatMoney(subtotal)}
                  </span>
                </div>

                {error && (
                  <p className={ALERT_ERROR} role="alert">
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
                <button
                  type="button"
                  onClick={clear}
                  disabled={submitting}
                  className="w-full text-center text-xs font-medium text-slate-500 transition hover:text-rose-600 disabled:opacity-50 dark:text-slate-400 dark:hover:text-rose-400"
                >
                  Vaciar carrito
                </button>
              </footer>
            </>
          )}
        </aside>
      </div>
    </Portal>
  )
}
