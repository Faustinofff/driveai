import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, Loader2, Pencil, Plus, ShieldAlert, Trash2, X } from 'lucide-react'
import { createCar, deleteCar, getCars, updateCar } from '../services/api'
import type { ApiError } from '../services/api'
import { selectIsAdmin, useAuthStore } from '../store/authStore'
import type { BodyType, Car, CarWriteRequest, FuelType, Transmission } from '../types'
import { BODY_LABELS, FUEL_LABELS } from '../types'
import { formatMoney, formatNumber } from '../utils/formatters'

const FUEL_OPTIONS: FuelType[] = ['GASOLINA', 'DIESEL', 'HIBRIDO', 'ELECTRICO']
const TRANSMISSION_OPTIONS: Transmission[] = ['MANUAL', 'AUTOMATICA']
const BODY_OPTIONS: BodyType[] = ['HATCHBACK', 'SEDAN', 'SUV', 'PICKUP']

interface FormDraft {
  brand: string
  model: string
  year: string
  price: string
  mileage: string
  fuelType: FuelType
  fuelConsumption: string
  transmission: Transmission
  bodyType: BodyType
  imageUrl: string
  description: string
  stock: string
}

const EMPTY_DRAFT: FormDraft = {
  brand: '',
  model: '',
  year: String(new Date().getFullYear()),
  price: '',
  mileage: '',
  fuelType: 'GASOLINA',
  fuelConsumption: '',
  transmission: 'MANUAL',
  bodyType: 'SEDAN',
  imageUrl: '',
  description: '',
  stock: '1',
}

function toDraft(car?: Car | null): FormDraft {
  if (!car) return EMPTY_DRAFT
  return {
    brand: car.brand,
    model: car.model,
    year: String(car.year),
    price: String(car.price),
    mileage: String(car.mileage),
    fuelType: car.fuelType,
    fuelConsumption: String(car.fuelConsumption),
    transmission: car.transmission,
    bodyType: car.bodyType,
    imageUrl: car.imageUrl,
    description: car.description,
    stock: String(car.stock),
  }
}

function toRequest(draft: FormDraft): CarWriteRequest {
  return {
    brand: draft.brand.trim(),
    model: draft.model.trim(),
    year: Number(draft.year),
    price: Number(draft.price),
    mileage: Number(draft.mileage),
    fuelType: draft.fuelType,
    fuelConsumption: Number(draft.fuelConsumption),
    transmission: draft.transmission,
    bodyType: draft.bodyType,
    imageUrl: draft.imageUrl.trim(),
    description: draft.description.trim(),
    stock: Number(draft.stock),
  }
}

interface CarFormModalProps {
  car: Car | null
  onClose: () => void
}

function CarFormModal({ car, onClose }: CarFormModalProps) {
  const queryClient = useQueryClient()
  const [draft, setDraft] = useState<FormDraft>(() => toDraft(car))
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const update = (patch: Partial<FormDraft>) => setDraft((current) => ({ ...current, ...patch }))

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (car) {
        await updateCar(car.id, toRequest(draft))
      } else {
        await createCar(toRequest(draft))
      }
      void queryClient.invalidateQueries({ queryKey: ['admin-cars'] })
      void queryClient.invalidateQueries({ queryKey: ['cars'] })
      onClose()
    } catch (err) {
      setError((err as ApiError).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm" onClick={onClose} />
      <form
        onSubmit={(event) => void handleSubmit(event)}
        className="relative max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl animate-fade-in-up scrollbar-none"
      >
        <header className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-bold">
            {car ? 'Editar auto' : 'Publicar nuevo auto'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="flex h-8 w-8 items-center justify-center rounded-full text-slate-500 transition hover:bg-slate-100 hover:text-slate-700"
            aria-label="Cerrar"
          >
            <X className="h-5 w-5" />
          </button>
        </header>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Marca</label>
            <input
              type="text"
              value={draft.brand}
              onChange={(event) => update({ brand: event.target.value })}
              placeholder="Ej. Toyota"
              required
              className="input-base"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Modelo</label>
            <input
              type="text"
              value={draft.model}
              onChange={(event) => update({ model: event.target.value })}
              placeholder="Ej. Corolla"
              required
              className="input-base"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Año</label>
            <input
              type="number"
              min={1900}
              max={2100}
              value={draft.year}
              onChange={(event) => update({ year: event.target.value })}
              required
              className="input-base"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Precio (USD)</label>
            <input
              type="number"
              min={0.01}
              step="0.01"
              value={draft.price}
              onChange={(event) => update({ price: event.target.value })}
              required
              className="input-base"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Kilometraje</label>
            <input
              type="number"
              min={0}
              value={draft.mileage}
              onChange={(event) => update({ mileage: event.target.value })}
              required
              className="input-base"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Stock</label>
            <input
              type="number"
              min={0}
              value={draft.stock}
              onChange={(event) => update({ stock: event.target.value })}
              required
              className="input-base"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Combustible</label>
            <select
              value={draft.fuelType}
              onChange={(event) => update({ fuelType: event.target.value as FuelType })}
              className="input-base"
            >
              {FUEL_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {FUEL_LABELS[option]}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">
              Consumo (L/100km)
            </label>
            <input
              type="number"
              min={0}
              step="0.1"
              value={draft.fuelConsumption}
              onChange={(event) => update({ fuelConsumption: event.target.value })}
              required
              className="input-base"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Transmisión</label>
            <select
              value={draft.transmission}
              onChange={(event) => update({ transmission: event.target.value as Transmission })}
              className="input-base"
            >
              {TRANSMISSION_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option === 'MANUAL' ? 'Manual' : 'Automática'}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Carrocería</label>
            <select
              value={draft.bodyType}
              onChange={(event) => update({ bodyType: event.target.value as BodyType })}
              className="input-base"
            >
              {BODY_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {BODY_LABELS[option]}
                </option>
              ))}
            </select>
          </div>
          <div className="sm:col-span-2">
            <label className="mb-1 block text-xs font-medium text-slate-500">URL de imagen</label>
            <input
              type="url"
              value={draft.imageUrl}
              onChange={(event) => update({ imageUrl: event.target.value })}
              placeholder="https://…"
              required
              className="input-base"
            />
          </div>
          <div className="sm:col-span-2">
            <label className="mb-1 block text-xs font-medium text-slate-500">Descripción</label>
            <textarea
              value={draft.description}
              onChange={(event) => update({ description: event.target.value })}
              rows={3}
              required
              className="input-base"
            />
          </div>
        </div>

        {error && (
          <p className="mt-4 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {error}
          </p>
        )}

        <footer className="mt-6 flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={busy}
            className="inline-flex items-center gap-2 rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white shadow-md shadow-brand-600/20 transition hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {busy && <Loader2 className="h-4 w-4 animate-spin" />}
            {busy ? 'Guardando…' : car ? 'Guardar cambios' : 'Publicar auto'}
          </button>
        </footer>
      </form>
    </div>
  )
}

export function AdminPage() {
  const isAdmin = useAuthStore(selectIsAdmin)
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<Car | null>(null)
  const [showForm, setShowForm] = useState(false)

  const carsQuery = useQuery({
    queryKey: ['admin-cars'],
    queryFn: () => getCars({ page: 0, size: 100 }),
    enabled: isAdmin,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteCar(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin-cars'] })
      void queryClient.invalidateQueries({ queryKey: ['cars'] })
    },
  })

  if (!isAdmin) {
    return (
      <div className="mx-auto flex max-w-md flex-col items-center px-4 py-20 text-center">
        <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-100 text-rose-600">
          <ShieldAlert className="h-7 w-7" />
        </span>
        <h1 className="mt-4 text-2xl font-extrabold tracking-tight">Acceso restringido</h1>
        <p className="mt-1 text-sm text-slate-500">
          Solo los administradores pueden gestionar el catálogo de autos.
        </p>
      </div>
    )
  }

  const handleDelete = (car: Car) => {
    if (window.confirm(`¿Eliminar «${car.brand} ${car.model} ${car.year}» del catálogo?`)) {
      deleteMutation.mutate(car.id)
    }
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight">Panel de administración</h1>
          <p className="text-sm text-slate-500">
            Publica, edita y elimina autos del catálogo.
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            setEditing(null)
            setShowForm(true)
          }}
          className="inline-flex items-center gap-2 rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white shadow-md shadow-brand-600/20 transition hover:bg-brand-700"
        >
          <Plus className="h-4 w-4" />
          Nuevo auto
        </button>
      </div>

      {deleteMutation.isError && (
        <p className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {(deleteMutation.error as ApiError).message}
        </p>
      )}

      {carsQuery.isPending && (
        <div className="flex items-center gap-2 text-sm text-slate-500">
          <Loader2 className="h-4 w-4 animate-spin" />
          Cargando inventario…
        </div>
      )}

      {carsQuery.isError && (
        <p className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {(carsQuery.error as ApiError).message}
        </p>
      )}

      {carsQuery.data && (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3 font-semibold">Auto</th>
                  <th className="px-4 py-3 font-semibold">Año</th>
                  <th className="px-4 py-3 font-semibold">Precio</th>
                  <th className="px-4 py-3 font-semibold">Stock</th>
                  <th className="px-4 py-3 text-right font-semibold">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {carsQuery.data.content.map((car) => (
                  <tr key={car.id} className="transition hover:bg-slate-50">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <img
                          src={car.imageUrl}
                          alt={`${car.brand} ${car.model}`}
                          loading="lazy"
                          className="h-12 w-16 shrink-0 rounded-lg object-cover"
                        />
                        <div>
                          <p className="font-semibold text-slate-900">
                            {car.brand} {car.model}
                          </p>
                          <p className="text-xs text-slate-500">
                            {BODY_LABELS[car.bodyType]} · {FUEL_LABELS[car.fuelType]}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">{car.year}</td>
                    <td className="px-4 py-3 font-semibold text-slate-900">
                      {formatMoney(car.price)}
                    </td>
                    <td className="px-4 py-3">
                      {car.stock > 0 ? (
                        <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-semibold text-emerald-700">
                          {formatNumber(car.stock)}
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2 py-0.5 text-xs font-semibold text-rose-700">
                          <AlertTriangle className="h-3 w-3" />
                          Agotado
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => {
                            setEditing(car)
                            setShowForm(true)
                          }}
                          className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:border-brand-400 hover:text-brand-700"
                        >
                          <Pencil className="h-3.5 w-3.5" />
                          Editar
                        </button>
                        <button
                          type="button"
                          disabled={deleteMutation.isPending}
                          onClick={() => handleDelete(car)}
                          className="inline-flex items-center gap-1.5 rounded-lg border border-rose-200 bg-rose-50 px-3 py-1.5 text-xs font-semibold text-rose-700 transition hover:bg-rose-100 disabled:opacity-50"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                          Eliminar
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {showForm && (
        <CarFormModal car={editing} onClose={() => setShowForm(false)} />
      )}
    </div>
  )
}