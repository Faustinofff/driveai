import { useState } from 'react'
import { Calendar, Check, Eye, Fuel, Gauge, Plus } from 'lucide-react'
import { useAuthStore } from '../store/authStore'
import { useCartStore } from '../store/cartStore'
import type { Car } from '../types'
import { CarDetailModal } from './CarDetailModal'
import {
  BODY_LABELS,
  FUEL_COLORS,
  FUEL_LABELS,
  TRANSMISSION_LABELS,
} from '../types'
import { formatConsumption, formatMileage, formatMoney } from '../utils/formatters'

interface CarCardProps {
  car: Car
  onRequireLogin?: () => void
}

export function CarCard({ car, onRequireLogin }: CarCardProps) {
  const add = useCartStore((state) => state.add)
  const isAuthenticated = useAuthStore((state) => state.token !== null && state.user !== null)
  const [imageLoaded, setImageLoaded] = useState(false)
  const [justAdded, setJustAdded] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const outOfStock = car.stock <= 0

  const handleAdd = () => {
    if (outOfStock) return
    if (!isAuthenticated) {
      onRequireLogin?.()
      return
    }
    add(car)
    setJustAdded(true)
    window.setTimeout(() => setJustAdded(false), 1200)
  }

  return (
    <article className="group flex flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-lg">
      <div className="relative aspect-[4/3] w-full overflow-hidden bg-slate-100">
        {!imageLoaded && <div className="absolute inset-0 skeleton" aria-hidden />}
        <img
          src={car.imageUrl}
          alt={`${car.brand} ${car.model} ${car.year}`}
          loading="lazy"
          onLoad={() => setImageLoaded(true)}
          className={`h-full w-full object-cover transition duration-500 group-hover:scale-105 ${
            imageLoaded ? 'opacity-100' : 'opacity-0'
          }`}
        />
        <span
          className={`absolute left-3 top-3 rounded-full border px-2.5 py-1 text-xs font-semibold ${FUEL_COLORS[car.fuelType]}`}
        >
          {FUEL_LABELS[car.fuelType]}
        </span>
        {outOfStock && (
          <span className="absolute right-3 top-3 rounded-full bg-slate-900/80 px-2.5 py-1 text-xs font-semibold text-white">
            Agotado
          </span>
        )}
        <button
          type="button"
          onClick={() => setDetailOpen(true)}
          className="absolute inset-0 flex items-end justify-end p-3 focus:outline-none"
          aria-label={`Ver detalle de ${car.brand} ${car.model}`}
        >
          <span className="inline-flex items-center gap-1.5 rounded-full bg-white/95 px-3 py-1.5 text-xs font-semibold text-slate-700 opacity-0 shadow-sm transition group-hover:opacity-100 focus:opacity-100">
            <Eye className="h-3.5 w-3.5" />
            Ver detalle
          </span>
        </button>
      </div>

      <div className="flex flex-1 flex-col p-4">
        <div className="flex items-start justify-between gap-2">
          <div>
            <h3 className="text-base font-bold text-slate-900">
              <button
                type="button"
                onClick={() => setDetailOpen(true)}
                className="text-left transition hover:text-brand-700"
              >
                {car.brand} {car.model}
              </button>
            </h3>
            <p className="text-sm text-slate-500">{BODY_LABELS[car.bodyType]}</p>
          </div>
          <span className="text-lg font-extrabold text-brand-700">
            {formatMoney(car.price)}
          </span>
        </div>

        <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-slate-500">
          <span className="inline-flex items-center gap-1">
            <Calendar className="h-3.5 w-3.5" />
            {car.year}
          </span>
          <span className="inline-flex items-center gap-1">
            <Gauge className="h-3.5 w-3.5" />
            {formatMileage(car.mileage)}
          </span>
          <span className="inline-flex items-center gap-1">
            <Fuel className="h-3.5 w-3.5" />
            {formatConsumption(car.fuelConsumption)}
          </span>
          <span className="rounded-md bg-slate-100 px-1.5 py-0.5 font-medium text-slate-600">
            {TRANSMISSION_LABELS[car.transmission]}
          </span>
        </div>

        <button
          type="button"
          disabled={outOfStock}
          onClick={handleAdd}
          className={`mt-4 inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition ${
            justAdded
              ? 'bg-emerald-600 text-white'
              : outOfStock
                ? 'cursor-not-allowed bg-slate-100 text-slate-400'
                : 'bg-brand-600 text-white shadow-md shadow-brand-600/20 hover:bg-brand-700 active:scale-[0.98]'
          }`}
        >
          {justAdded ? <Check className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
          {justAdded ? 'Agregado' : outOfStock ? 'Sin stock' : 'Agregar al carrito'}
        </button>
      </div>

      {detailOpen && (
        <CarDetailModal
          car={car}
          added={justAdded}
          onClose={() => setDetailOpen(false)}
          onAdd={handleAdd}
        />
      )}
    </article>
  )
}

export function CarCardSkeleton() {
  return (
    <article className="flex flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="aspect-[4/3] w-full skeleton" />
      <div className="flex flex-1 flex-col gap-3 p-4">
        <div className="h-4 w-2/3 skeleton rounded" />
        <div className="h-4 w-1/4 skeleton rounded" />
        <div className="h-4 w-full skeleton rounded" />
        <div className="mt-2 h-10 w-full skeleton rounded-xl" />
      </div>
    </article>
  )
}