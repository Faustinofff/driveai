import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertCircle, ClipboardList, Loader2, RefreshCw, XCircle } from 'lucide-react'
import { CarImage } from '../components/CarImage'
import { cancelOrder, listOrders } from '../services/api'
import type { ApiError } from '../services/api'
import type { Order } from '../types'
import { ORDER_STATUS_COLORS, ORDER_STATUS_LABELS } from '../types'
import type { AppView } from '../types/view'
import { formatDate, formatMoney, formatNumber } from '../utils/formatters'
import { ALERT_ERROR, BTN_DANGER, BTN_PRIMARY, BTN_SECONDARY, PANEL } from '../utils/styles'

interface OrdersPageProps {
  onNavigate: (view: AppView) => void
}

function OrderCard({ order }: { order: Order }) {
  const queryClient = useQueryClient()
  const [confirming, setConfirming] = useState(false)

  const cancel = useMutation({
    mutationFn: () => cancelOrder(order.id),
    onSuccess: (updated) => {
      queryClient.setQueryData<Order[]>(['orders'], (previous) =>
        previous?.map((item) => (item.id === updated.id ? updated : item)),
      )
      // Al cancelar vuelve el stock: el catálogo y el panel deben reflejarlo.
      void queryClient.invalidateQueries({ queryKey: ['cars'] })
      void queryClient.invalidateQueries({ queryKey: ['admin-cars'] })
      void queryClient.invalidateQueries({ queryKey: ['home-featured'] })
      setConfirming(false)
    },
  })

  const cancellable = order.status !== 'CANCELLED'
  const units = order.items.reduce((total, item) => total + item.quantity, 0)

  return (
    <article className={`${PANEL} overflow-hidden`} aria-labelledby={`order-${order.id}-title`}>
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-5 py-4 dark:border-slate-800">
        <div>
          <h2 id={`order-${order.id}-title`} className="text-base font-bold text-slate-900 dark:text-white">
            Orden #{order.id}
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            {formatDate(order.createdAt)} · {formatNumber(units)} {units === 1 ? 'unidad' : 'unidades'}
          </p>
        </div>
        <span
          className={`rounded-full border px-3 py-1 text-xs font-semibold ${ORDER_STATUS_COLORS[order.status]}`}
          data-testid={`order-${order.id}-status`}
        >
          {ORDER_STATUS_LABELS[order.status]}
        </span>
      </header>

      <ul className="divide-y divide-slate-100 px-5 dark:divide-slate-800">
        {order.items.map((item) => (
          <li key={item.id} className="flex items-center gap-3 py-3">
            <CarImage
              src={item.car.imageUrl}
              alt={`${item.car.brand} ${item.car.model}`}
              className="h-14 w-20 shrink-0 rounded-lg"
              compact
            />
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold text-slate-900 dark:text-white">
                {item.car.brand} {item.car.model}
              </p>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {formatNumber(item.quantity)} × {formatMoney(item.unitPrice)}
              </p>
            </div>
            <span className="shrink-0 text-sm font-bold text-slate-800 dark:text-slate-100">
              {formatMoney(item.lineTotal)}
            </span>
          </li>
        ))}
      </ul>

      <footer className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 bg-slate-50 px-5 py-4 dark:border-slate-800 dark:bg-slate-800/60">
        <div>
          <span className="text-xs text-slate-500 dark:text-slate-400">Total</span>
          <p className="text-lg font-extrabold text-brand-700 dark:text-brand-300">
            {formatMoney(order.totalAmount)}
          </p>
        </div>

        {cancellable && !confirming && (
          <button
            type="button"
            onClick={() => setConfirming(true)}
            className={`${BTN_DANGER} px-4 py-2`}
          >
            <XCircle className="h-4 w-4" />
            Cancelar pedido
          </button>
        )}

        {cancellable && confirming && (
          <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Confirmar cancelación">
            <span className="text-sm text-slate-600 dark:text-slate-300">
              ¿Cancelar la orden #{order.id}? Se devuelve el stock.
            </span>
            <button
              type="button"
              onClick={() => cancel.mutate()}
              disabled={cancel.isPending}
              className={`${BTN_DANGER} px-3 py-1.5 text-xs`}
            >
              {cancel.isPending && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
              Sí, cancelar
            </button>
            <button
              type="button"
              onClick={() => setConfirming(false)}
              disabled={cancel.isPending}
              className={`${BTN_SECONDARY} px-3 py-1.5 text-xs`}
            >
              Mantener
            </button>
          </div>
        )}
      </footer>

      {cancel.isError && (
        <p className={`${ALERT_ERROR} mx-5 mb-4`} role="alert">
          {(cancel.error as ApiError).message}
        </p>
      )}
    </article>
  )
}

export function OrdersPage({ onNavigate }: OrdersPageProps) {
  const orders = useQuery({ queryKey: ['orders'], queryFn: listOrders })

  return (
    <div className="mx-auto max-w-4xl px-4 py-6">
      <div className="mb-6 flex items-center gap-3">
        <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-brand-600 text-white shadow-lg shadow-brand-600/25">
          <ClipboardList className="h-6 w-6" />
        </span>
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight">Mis pedidos</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Historial de compras y cancelación de órdenes confirmadas.
          </p>
        </div>
      </div>

      {orders.isPending && (
        <div className="space-y-4" aria-busy="true" aria-label="Cargando pedidos">
          {Array.from({ length: 2 }, (_, index) => (
            <div key={index} className={`${PANEL} p-5`}>
              <div className="h-4 w-1/3 skeleton rounded" />
              <div className="mt-3 h-14 w-full skeleton rounded-lg" />
              <div className="mt-3 h-4 w-1/4 skeleton rounded" />
            </div>
          ))}
        </div>
      )}

      {orders.isError && (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-rose-200 bg-rose-50 px-6 py-16 text-center dark:border-rose-500/40 dark:bg-rose-500/10">
          <AlertCircle className="h-10 w-10 text-rose-500 dark:text-rose-400" />
          <p className="mt-3 font-semibold text-rose-700 dark:text-rose-300">
            No se pudieron cargar tus pedidos
          </p>
          <p className="mt-1 text-sm text-rose-500 dark:text-rose-300/80">
            {(orders.error as ApiError).message}
          </p>
          <button
            type="button"
            onClick={() => void orders.refetch()}
            className="mt-5 inline-flex items-center gap-2 rounded-xl bg-rose-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-rose-700"
          >
            <RefreshCw className="h-4 w-4" />
            Reintentar
          </button>
        </div>
      )}

      {orders.isSuccess && orders.data.length === 0 && (
        <div className={`${PANEL} flex flex-col items-center px-6 py-16 text-center`}>
          <ClipboardList className="h-10 w-10 text-slate-400 dark:text-slate-500" />
          <p className="mt-3 font-semibold text-slate-700 dark:text-slate-200">
            Todavía no hiciste ningún pedido
          </p>
          <p className="mt-1 max-w-sm text-sm text-slate-500 dark:text-slate-400">
            Cuando finalices una compra la vas a ver acá con su estado y el detalle de unidades.
          </p>
          <button
            type="button"
            onClick={() => onNavigate('catalog')}
            className={`${BTN_PRIMARY} mt-5`}
          >
            Ir al catálogo
          </button>
        </div>
      )}

      {orders.isSuccess && orders.data.length > 0 && (
        <div className="space-y-4">
          {orders.data.map((order) => (
            <OrderCard key={order.id} order={order} />
          ))}
        </div>
      )}
    </div>
  )
}
