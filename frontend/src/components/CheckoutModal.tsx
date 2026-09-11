import { CheckCircle2, ShoppingBag, X } from 'lucide-react'
import type { Order } from '../types'
import { formatMoney, formatNumber } from '../utils/formatters'

interface CheckoutModalProps {
  order: Order | null
  onClose: () => void
}

export function CheckoutModal({ order, onClose }: CheckoutModalProps) {
  if (!order) return null

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center p-4" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm" onClick={onClose} />

      <div className="relative w-full max-w-md overflow-hidden rounded-3xl bg-white shadow-2xl animate-fade-in-up">
        <div className="bg-emerald-600 px-6 pb-8 pt-6 text-center text-white">
          <CheckCircle2 className="mx-auto h-14 w-14" />
          <h2 className="mt-2 text-xl font-extrabold">¡Pedido confirmado!</h2>
          <p className="mt-1 text-sm text-emerald-100">
            Orden #{order.id} · Estado:{' '}
            <span className="font-bold uppercase tracking-wide">{order.status}</span>
          </p>
        </div>

        <div className="px-6 py-5">
          <p className="text-sm font-semibold text-slate-500">Resumen</p>
          <ul className="mt-3 space-y-2">
            {order.items.map((item) => (
              <li
                key={item.id}
                className="flex items-center justify-between gap-3 text-sm"
              >
                <span className="truncate text-slate-700">
                  {item.car.brand} {item.car.model}
                  <span className="ml-2 text-xs text-slate-400">
                    x{formatNumber(item.quantity)}
                  </span>
                </span>
                <span className="shrink-0 font-semibold text-slate-800">
                  {formatMoney(item.lineTotal)}
                </span>
              </li>
            ))}
          </ul>

          <div className="mt-4 flex items-center justify-between border-t border-slate-200 pt-4">
            <span className="font-semibold text-slate-700">Total</span>
            <span className="text-xl font-extrabold text-brand-700">
              {formatMoney(order.totalAmount)}
            </span>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 py-3 text-sm font-bold text-white transition hover:bg-brand-700"
          >
            <ShoppingBag className="h-4 w-4" />
            Seguir comprando
          </button>

          <button
            type="button"
            onClick={onClose}
            className="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-full bg-white/10 text-white transition hover:bg-white/20"
            aria-label="Cerrar"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  )
}