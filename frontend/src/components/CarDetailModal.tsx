import { useEffect } from 'react'
import { Calendar, Check, Fuel, Gauge, Plus, ShieldCheck, X } from 'lucide-react'
import type { Car } from '../types'
import {
  BODY_LABELS,
  FUEL_COLORS,
  FUEL_LABELS,
  TRANSMISSION_LABELS,
} from '../types'
import { formatConsumption, formatMileage, formatMoney, formatNumber } from '../utils/formatters'

interface CarDetailModalProps {
  car: Car
  onClose: () => void
  onAdd?: () => void
  added?: boolean
}

/**
 * Detalle de vehículo del catálogo. Se abre desde la tarjeta sin recargar la
 * página (SPA con navegación por hash). Accesible por teclado y lector de
 * pantalla: rol dialog, aria-modal y cierre con Escape.
 */
export function CarDetailModal({ car, onClose, onAdd, added }: CarDetailModalProps) {
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  const outOfStock = car.stock <= 0

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="car-detail-title"
    >
      <div className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm" onClick={onClose} />

      <div className="relative max-h-[92vh] w-full max-w-3xl overflow-y-auto rounded-3xl bg-white shadow-2xl animate-fade-in-up scrollbar-none">
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 z-10 flex h-9 w-9 items-center justify-center rounded-full bg-white/90 text-slate-600 shadow-sm transition hover:bg-white hover:text-slate-900"
          aria-label="Cerrar detalle"
        >
          <X className="h-5 w-5" />
        </button>

        <div className="relative aspect-[16/9] w-full overflow-hidden bg-slate-100">
          <img
            src={car.imageUrl}
            alt={`${car.brand} ${car.model} ${car.year}`}
            className="h-full w-full object-cover"
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
              <p className="text-sm text-slate-500">
                {car.year} · {BODY_LABELS[car.bodyType]} ·{' '}
                {TRANSMISSION_LABELS[car.transmission]}
              </p>
            </div>
            <div className="text-right">
              <p className="text-2xl font-extrabold text-brand-700">{formatMoney(car.price)}</p>
              <p className="text-xs text-slate-500">
                {outOfStock
                  ? 'Sin stock'
                  : `${formatNumber(car.stock)} unidad${car.stock === 1 ? '' : 'es'} disponible${car.stock === 1 ? '' : 's'}`}
              </p>
            </div>
          </div>

          <p className="mt-4 text-sm leading-relaxed text-slate-600">{car.description}</p>

          <dl className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div className="rounded-2xl border border-slate-200 p-3">
              <dt className="flex items-center gap-1 text-xs text-slate-500">
                <Calendar className="h-3.5 w-3.5" /> Año
              </dt>
              <dd className="mt-1 font-bold text-slate-800">{car.year}</dd>
            </div>
            <div className="rounded-2xl border border-slate-200 p-3">
              <dt className="flex items-center gap-1 text-xs text-slate-500">
                <Gauge className="h-3.5 w-3.5" /> Kilometraje
              </dt>
              <dd className="mt-1 font-bold text-slate-800">{formatMileage(car.mileage)}</dd>
            </div>
            <div className="rounded-2xl border border-slate-200 p-3">
              <dt className="flex items-center gap-1 text-xs text-slate-500">
                <Fuel className="h-3.5 w-3.5" /> Consumo
              </dt>
              <dd className="mt-1 font-bold text-slate-800">
                {formatConsumption(car.fuelConsumption)}
              </dd>
            </div>
            <div className="rounded-2xl border border-slate-200 p-3">
              <dt className="flex items-center gap-1 text-xs text-slate-500">
                <ShieldCheck className="h-3.5 w-3.5" /> Transmisión
              </dt>
              <dd className="mt-1 font-bold text-slate-800">
                {TRANSMISSION_LABELS[car.transmission]}
              </dd>
            </div>
          </dl>

          <div className="mt-6 flex flex-wrap items-center gap-3">
            <button
              type="button"
              disabled={outOfStock || !onAdd}
              onClick={onAdd}
              className={`inline-flex items-center justify-center gap-2 rounded-xl px-5 py-3 text-sm font-bold transition ${
                added
                  ? 'bg-emerald-600 text-white'
                  : outOfStock
                    ? 'cursor-not-allowed bg-slate-100 text-slate-400'
                    : 'bg-brand-600 text-white shadow-lg shadow-brand-600/25 hover:bg-brand-700 active:scale-[0.98]'
              }`}
            >
              {added ? <Check className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
              {added ? 'Agregado al carrito' : outOfStock ? 'Sin stock' : 'Agregar al carrito'}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl border border-slate-300 px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-brand-400 hover:text-brand-700"
            >
              Volver al catálogo
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
