import { Calendar, Check, Fuel, Gauge, PackageX, Plus, ShieldCheck, X } from 'lucide-react'
import { useModalBehavior } from '../hooks/useModalBehavior'
import type { Car } from '../types'
import {
  BODY_LABELS,
  FUEL_COLORS,
  FUEL_LABELS,
  TRANSMISSION_LABELS,
} from '../types'
import { formatConsumption, formatMileage, formatMoney, formatNumber } from '../utils/formatters'
import { BTN_SECONDARY, OVERLAY } from '../utils/styles'
import type { AddFeedback } from './CarCard'
import { CarImage } from './CarImage'
import { Portal } from './Portal'

interface CarDetailModalProps {
  car: Car
  onClose: () => void
  onAdd?: () => void
  feedback?: AddFeedback
}

/**
 * Detalle de vehículo del catálogo. Se abre desde la tarjeta sin recargar la
 * página (SPA con navegación por hash). Accesible por teclado y lector de
 * pantalla: rol dialog, aria-modal, foco inicial y cierre con Escape.
 *
 * Se monta en un portal bajo <body>: dentro de la tarjeta (que tiene
 * `transform` al hacer hover) el `position: fixed` quedaba relativo a la
 * tarjeta y el detalle se dibujaba recortado dentro de ella.
 */
export function CarDetailModal({ car, onClose, onAdd, feedback = 'idle' }: CarDetailModalProps) {
  const panelRef = useModalBehavior<HTMLDivElement>(onClose)
  const outOfStock = car.stock <= 0

  const addLabel =
    feedback === 'added'
      ? 'Agregado al carrito'
      : feedback === 'limit'
        ? 'Máximo en carrito'
        : outOfStock
          ? 'Sin stock'
          : 'Agregar al carrito'

  const addClass =
    feedback === 'added'
      ? 'bg-emerald-600 text-white'
      : feedback === 'limit'
        ? 'bg-amber-500 text-white'
        : outOfStock
          ? 'cursor-not-allowed bg-slate-100 text-slate-400 dark:bg-slate-800 dark:text-slate-500'
          : 'bg-brand-600 text-white shadow-lg shadow-brand-600/25 hover:bg-brand-700 active:scale-[0.98]'

  return (
    <Portal>
      <div
        className="fixed inset-0 z-50 flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-labelledby="car-detail-title"
      >
        <button
          type="button"
          tabIndex={-1}
          aria-label="Fondo del detalle"
          onClick={onClose}
          className={`${OVERLAY} cursor-default`}
        />

        <div
          ref={panelRef}
          tabIndex={-1}
          className="relative max-h-[92vh] w-full max-w-3xl overflow-y-auto rounded-3xl bg-white text-slate-900 shadow-2xl outline-none animate-fade-in-up scrollbar-none dark:bg-slate-900 dark:text-slate-100"
        >
          <button
            type="button"
            onClick={onClose}
            className="absolute right-4 top-4 z-10 flex h-9 w-9 items-center justify-center rounded-full bg-white/90 text-slate-600 shadow-sm transition hover:bg-white hover:text-slate-900 dark:bg-slate-900/90 dark:text-slate-200 dark:hover:bg-slate-800 dark:hover:text-white"
            aria-label="Cerrar detalle"
          >
            <X className="h-5 w-5" />
          </button>

          <div className="relative">
            <CarImage
              src={car.imageUrl}
              alt={`${car.brand} ${car.model} ${car.year}`}
              className="aspect-[16/9] w-full"
              eager
            />
            <span
              className={`absolute left-4 top-4 rounded-full border px-3 py-1 text-xs font-semibold ${FUEL_COLORS[car.fuelType]}`}
            >
              {FUEL_LABELS[car.fuelType]}
            </span>
          </div>

          <div className="p-6">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 id="car-detail-title" className="text-2xl font-extrabold tracking-tight">
                  {car.brand} {car.model}
                </h2>
                <p className="text-sm text-slate-500 dark:text-slate-400">
                  {car.year} · {BODY_LABELS[car.bodyType]} ·{' '}
                  {TRANSMISSION_LABELS[car.transmission]}
                </p>
              </div>
              <div className="text-right">
                <p className="text-2xl font-extrabold text-brand-700 dark:text-brand-300">
                  {formatMoney(car.price)}
                </p>
                <p
                  className={`text-xs ${
                    outOfStock
                      ? 'font-semibold text-rose-600 dark:text-rose-400'
                      : car.stock <= 2
                        ? 'font-semibold text-amber-600 dark:text-amber-400'
                        : 'text-slate-500 dark:text-slate-400'
                  }`}
                >
                  {outOfStock
                    ? 'Sin stock'
                    : `${formatNumber(car.stock)} unidad${car.stock === 1 ? '' : 'es'} disponible${car.stock === 1 ? '' : 's'}`}
                </p>
              </div>
            </div>

            <p className="mt-4 text-sm leading-relaxed text-slate-600 dark:text-slate-300">
              {car.description}
            </p>

            <dl className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
              {[
                { Icon: Calendar, label: 'Año', value: String(car.year) },
                { Icon: Gauge, label: 'Kilometraje', value: formatMileage(car.mileage) },
                { Icon: Fuel, label: 'Consumo', value: formatConsumption(car.fuelConsumption) },
                { Icon: ShieldCheck, label: 'Transmisión', value: TRANSMISSION_LABELS[car.transmission] },
              ].map(({ Icon, label, value }) => (
                <div
                  key={label}
                  className="rounded-2xl border border-slate-200 p-3 dark:border-slate-700"
                >
                  <dt className="flex items-center gap-1 text-xs text-slate-500 dark:text-slate-400">
                    <Icon className="h-3.5 w-3.5" /> {label}
                  </dt>
                  <dd className="mt-1 font-bold text-slate-800 dark:text-slate-100">{value}</dd>
                </div>
              ))}
            </dl>

            <div className="mt-6 flex flex-wrap items-center gap-3">
              <button
                type="button"
                disabled={outOfStock || !onAdd}
                onClick={onAdd}
                aria-live="polite"
                className={`inline-flex items-center justify-center gap-2 rounded-xl px-5 py-3 text-sm font-bold transition ${addClass}`}
              >
                {feedback === 'added' ? (
                  <Check className="h-4 w-4" />
                ) : feedback === 'limit' ? (
                  <PackageX className="h-4 w-4" />
                ) : (
                  <Plus className="h-4 w-4" />
                )}
                {addLabel}
              </button>
              <button type="button" onClick={onClose} className={`${BTN_SECONDARY} px-5 py-3`}>
                Volver al catálogo
              </button>
            </div>
          </div>
        </div>
      </div>
    </Portal>
  )
}
