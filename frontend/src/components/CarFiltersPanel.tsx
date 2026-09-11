import { useEffect, useState } from 'react'
import { FilterX } from 'lucide-react'
import type { BodyType, CarFilters, FuelType } from '../types'
import { BODY_LABELS, FUEL_LABELS } from '../types'

interface CarFiltersProps {
  filters: CarFilters
  onChange: (filters: CarFilters) => void
  onClear: () => void
}

interface Draft {
  brand: string
  minPrice: string
  maxPrice: string
  minYear: string
  fuelType: string
  bodyType: string
}

function toDraft(filters: CarFilters): Draft {
  return {
    brand: filters.brand ?? '',
    minPrice: filters.minPrice?.toString() ?? '',
    maxPrice: filters.maxPrice?.toString() ?? '',
    minYear: filters.minYear?.toString() ?? '',
    fuelType: filters.fuelType ?? '',
    bodyType: filters.bodyType ?? '',
  }
}

function toFilters(draft: Draft): CarFilters {
  const filters: CarFilters = {}
  if (draft.brand.trim()) filters.brand = draft.brand.trim()
  const minPrice = parseFloat(draft.minPrice)
  if (!Number.isNaN(minPrice)) filters.minPrice = minPrice
  const maxPrice = parseFloat(draft.maxPrice)
  if (!Number.isNaN(maxPrice)) filters.maxPrice = maxPrice
  const minYear = parseInt(draft.minYear, 10)
  if (!Number.isNaN(minYear)) filters.minYear = minYear
  if (draft.fuelType) filters.fuelType = draft.fuelType as FuelType
  if (draft.bodyType) filters.bodyType = draft.bodyType as BodyType
  return filters
}

const FUEL_OPTIONS: (FuelType | '')[] = ['', 'GASOLINA', 'DIESEL', 'HIBRIDO', 'ELECTRICO']
const BODY_OPTIONS: (BodyType | '')[] = ['', 'HATCHBACK', 'SEDAN', 'SUV', 'PICKUP']

export function CarFiltersPanel({ filters, onChange, onClear }: CarFiltersProps) {
  const [draft, setDraft] = useState<Draft>(() => toDraft(filters))
  const snapshot = JSON.stringify(filters)

  useEffect(() => {
    setDraft(toDraft(filters))
  }, [snapshot])

  const update = (patch: Partial<Draft>) => {
    const next = { ...draft, ...patch }
    setDraft(next)
    onChange(toFilters(next))
  }

  const clearAll = () => {
    setDraft({ brand: '', minPrice: '', maxPrice: '', minYear: '', fuelType: '', bodyType: '' })
    onClear()
  }

  const hasActiveFilters =
    filters.brand || filters.minPrice !== undefined || filters.maxPrice !== undefined ||
    filters.minYear !== undefined || filters.fuelType || filters.bodyType

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-sm font-bold tracking-wide text-slate-700">Filtros</h2>
        {hasActiveFilters && (
          <button
            type="button"
            onClick={clearAll}
            className="inline-flex items-center gap-1 text-xs font-medium text-slate-500 transition hover:text-brand-700"
          >
            <FilterX className="h-3.5 w-3.5" />
            Limpiar
          </button>
        )}
      </div>

      <div className="space-y-3">
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">Marca</label>
          <input
            type="text"
            value={draft.brand}
            onChange={(event) => update({ brand: event.target.value })}
            placeholder="Ej. Toyota"
            className="input-base"
          />
        </div>

        <div className="grid grid-cols-2 gap-2">
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Precio min.</label>
            <input
              type="number"
              min={0}
              inputMode="numeric"
              value={draft.minPrice}
              onChange={(event) => update({ minPrice: event.target.value })}
              placeholder="0"
              className="input-base"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Precio máx.</label>
            <input
              type="number"
              min={0}
              inputMode="numeric"
              value={draft.maxPrice}
              onChange={(event) => update({ maxPrice: event.target.value })}
              placeholder="50k"
              className="input-base"
            />
          </div>
        </div>

        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">Año mínimo</label>
          <input
            type="number"
            min={1900}
            max={2100}
            inputMode="numeric"
            value={draft.minYear}
            onChange={(event) => update({ minYear: event.target.value })}
            placeholder="2020"
            className="input-base"
          />
        </div>

        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">Combustible</label>
          <select
            value={draft.fuelType}
            onChange={(event) => update({ fuelType: event.target.value })}
            className="input-base"
          >
            {FUEL_OPTIONS.map((option) => (
              <option key={option || 'any-fuel'} value={option}>
                {option ? FUEL_LABELS[option as FuelType] : 'Cualquiera'}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="mb-1 block text-xs font-medium text-slate-500">Carrocería</label>
          <select
            value={draft.bodyType}
            onChange={(event) => update({ bodyType: event.target.value })}
            className="input-base"
          >
            {BODY_OPTIONS.map((option) => (
              <option key={option || 'any-body'} value={option}>
                {option ? BODY_LABELS[option as BodyType] : 'Cualquiera'}
              </option>
            ))}
          </select>
        </div>
      </div>
    </div>
  )
}