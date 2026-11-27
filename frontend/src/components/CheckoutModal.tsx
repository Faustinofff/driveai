import { CheckCircle2, ClipboardList, ShoppingBag, X } from 'lucide-react'
import { useModalBehavior } from '../hooks/useModalBehavior'
import type { Order } from '../types'
import { formatMoney, formatNumber } from '../utils/formatters'
import { BTN_SECONDARY } from '../utils/styles'
import { Portal } from './Portal'

interface CheckoutModalProps {
  order: Order | null
  onClose: () => void
  onViewOrders?: () => void
}

export function CheckoutModal({ order, onClose, onViewOrders }: CheckoutModalProps) {
  if (!order) return null
  return <CheckoutModalPanel order={order} onClose={onClose} onViewOrders={onViewOrders} />
}

function CheckoutModalPanel({
  order,
  onClose,
  onViewOrders,
}: Omit<CheckoutModalProps, 'order'> & { order: Order }) {
  const panelRef = useModalBehavior<HTMLDivElement>(onClose)

  return (
    <Portal>
      <div
        className="fixed inset-0 z-[60] flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-labelledby="checkout-title"
      >
        <button
          type="button"
          tabIndex={-1}
          aria-label="Cerrar confirmación del pedido"
          onClick={onClose}
          className="absolute inset-0 cursor-default bg-slate-900/60 backdrop-blur-sm"
        />

        <div
          ref={panelRef}
          tabIndex={-1}
          className="relative w-full max-w-md overflow-hidden rounded-3xl bg-white text-slate-900 shadow-2xl outline-none animate-fade-in-up dark:bg-slate-900 dark:text-slate-100"
        >
          <div className="bg-emerald-600 px-6 pb-8 pt-6 text-center text-white">
            <CheckCircle2 className="mx-auto h-14 w-14" />
            <h2 id="checkout-title" className="mt-2 text-xl font-extrabold">
              ¡Pedido confirmado!
            </h2>
            <p className="mt-1 text-sm text-emerald-100">
              Orden #{order.id} · Estado:{' '}
              <span className="font-bold uppercase tracking-wide">{order.status}</span>
            </p>
          </div>

          <div className="px-6 py-5">
            <p className="text-sm font-semibold text-slate-500 dark:text-slate-400">Resumen</p>
            <ul className="mt-3 space-y-2">
              {order.items.map((item) => (
                <li
                  key={item.id}
                  className="flex items-center justify-between gap-3 text-sm"
                >
                  <span className="truncate text-slate-700 dark:text-slate-200">
                    {item.car.brand} {item.car.model}
                    <span className="ml-2 text-xs text-slate-400 dark:text-slate-500">
                      x{formatNumber(item.quantity)}
                    </span>
                  </span>
                  <span className="shrink-0 font-semibold text-slate-800 dark:text-slate-100">
                    {formatMoney(item.lineTotal)}
                  </span>
                </li>
              ))}
            </ul>

            <div className="mt-4 flex items-center justify-between border-t border-slate-200 pt-4 dark:border-slate-800">
              <span className="font-semibold text-slate-700 dark:text-slate-200">Total</span>
              <span className="text-xl font-extrabold text-brand-700 dark:text-brand-300">
                {formatMoney(order.totalAmount)}
              </span>
            </div>

            <p className="mt-3 text-xs text-slate-500 dark:text-slate-400">
              El stock ya quedó reservado. Podés revisar o cancelar este pedido desde{' '}
              <span className="font-semibold">Mis pedidos</span>.
            </p>

            <button
              type="button"
              onClick={onClose}
              className="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 py-3 text-sm font-bold text-white transition hover:bg-brand-700"
            >
              <ShoppingBag className="h-4 w-4" />
              Seguir comprando
            </button>
            {onViewOrders && (
              <button
                type="button"
                onClick={onViewOrders}
                className={`${BTN_SECONDARY} mt-2 w-full py-3`}
              >
                <ClipboardList className="h-4 w-4" />
                Ver mis pedidos
              </button>
            )}

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
    </Portal>
  )
}
