import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, Loader2, LogOut, Pencil, Plus, ShieldAlert, Trash2, X } from 'lucide-react'
import { CarImage } from '../components/CarImage'
import { Portal } from '../components/Portal'
import { useModalBehavior } from '../hooks/useModalBehavior'
import { createCar, deleteCar, getCars, updateCar } from '../services/api'
import type { ApiError } from '../services/api'
import { selectIsAdmin, selectLogout, useAuthStore } from '../store/authStore'
import type { BodyType, Car, CarWriteRequest, FuelType, Transmission } from '../types'
import { BODY_LABELS, FUEL_LABELS } from '../types'
import { formatMoney, formatNumber } from '../utils/formatters'
import { ALERT_ERROR, BTN_PRIMARY, BTN_SECONDARY, ICON_BUTTON, OVERLAY, PANEL } from '../utils/styles'

const ADMIN_PAGE_SIZE = 50

/** El backend limita cada página a 50: se recorren todas para listar el inventario completo. */
async function getAllCars(): Promise<Car[]> {
  const first = await getCars({ page: 0, size: ADMIN_PAGE_SIZE })
  const content = [...first.content]
  for (let page = 1; page < first.totalPages; page += 1) {
    const next = await getCars({ page, size: ADMIN_PAGE_SIZE })
    content.push(...next.content)
  }
  return content
}

const LABEL_CLASS = 'mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400'

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
  const panelRef = useModalBehavior<HTMLFormElement>(onClose)

  const update = (patch: Partial<FormDraft>) => setDraft((current) => ({ ...current, ...patch }))

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (car) {
        await updateCar(car.id, toRequest(draft), car.version)
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
    <Portal>
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="car-form-title"
    >
      <div className={OVERLAY} onClick={onClose} />
      <form
        ref={panelRef}
        tabIndex={-1}
        onSubmit={(event) => void handleSubmit(event)}
        className="relative max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-6 text-slate-900 shadow-2xl outline-none animate-fade-in-up scrollbar-none dark:bg-slate-900 dark:text-slate-100"
      >
        <header className="mb-5 flex items-center justify-between">
          <h2 id="car-form-title" className="text-lg font-bold">
            {car ? 'Editar auto' : 'Publicar nuevo auto'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className={ICON_BUTTON}
            aria-label="Cerrar"
          >
            <X className="h-5 w-5" />
          </button>
        </header>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="car-brand" className={LABEL_CLASS}>Marca</label>
            <input
              id="car-brand"
              type="text"
              value={draft.brand}
              onChange={(event) => update({ brand: event.target.value })}
              placeholder="Ej. Toyota"
              required
              className="input-base"
            />
          </div>
          <div>
            <label htmlFor="car-model" className={LABEL_CLASS}>Modelo</label>
            <input
              id="car-model"
              type="text"
              value={draft.model}
              onChange={(event) => update({ model: event.target.value })}
              placeholder="Ej. Corolla"
              required
              className="input-base"
            />
          </div>
          <div>
            <label htmlFor="car-year" className={LABEL_CLASS}>Año</label>
            <input
              id="car-year"
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
            <label htmlFor="car-price" className={LABEL_CLASS}>Precio (USD)</label>
            <input
              id="car-price"
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
            <label htmlFor="car-mileage" className={LABEL_CLASS}>Kilometraje</label>
            <input
              id="car-mileage"
              type="number"
              min={0}
              value={draft.mileage}
              onChange={(event) => update({ mileage: event.target.value })}
              required
              className="input-base"
            />
          </div>
          <div>
            <label htmlFor="car-stock" className={LABEL_CLASS}>Stock</label>
            <input
              id="car-stock"
              type="number"
              min={0}
              value={draft.stock}
              onChange={(event) => update({ stock: event.target.value })}
              required
              className="input-base"
            />
          </div>
          <div>
            <label htmlFor="car-fuel" className={LABEL_CLASS}>Combustible</label>
            <select
              id="car-fuel"
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
            <label htmlFor="car-consumption" className={LABEL_CLASS}>
              Consumo (L/100km)
            </label>
            <input
              id="car-consumption"
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
            <label htmlFor="car-transmission" className={LABEL_CLASS}>Transmisión</label>
            <select
              id="car-transmission"
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
            <label htmlFor="car-body" className={LABEL_CLASS}>Carrocería</label>
            <select
              id="car-body"
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
            <label htmlFor="car-image" className={LABEL_CLASS}>URL de imagen</label>
            <input
              id="car-image"
              type="url"
              value={draft.imageUrl}
              onChange={(event) => update({ imageUrl: event.target.value })}
              placeholder="https://…"
              required
              className="input-base"
            />
          </div>
          <div className="sm:col-span-2">
            <label htmlFor="car-description" className={LABEL_CLASS}>Descripción</label>
            <textarea
              id="car-description"
              value={draft.description}
              onChange={(event) => update({ description: event.target.value })}
              rows={3}
              required
              className="input-base"
            />
          </div>
        </div>

        {error && (
          <p className={`${ALERT_ERROR} mt-4`} role="alert">
            {error}
          </p>
        )}

        <footer className="mt-6 flex justify-end gap-2">
          <button type="button" onClick={onClose} className={`${BTN_SECONDARY} py-2`}>
            Cancelar
          </button>
          <button type="submit" disabled={busy} className={`${BTN_PRIMARY} py-2`}>
            {busy && <Loader2 className="h-4 w-4 animate-spin" />}
            {busy ? 'Guardando…' : car ? 'Guardar cambios' : 'Publicar auto'}
          </button>
        </footer>
      </form>
    </div>
    </Portal>
  )
}

export function AdminPage() {
  const isAdmin = useAuthStore(selectIsAdmin)
  const logout = useAuthStore(selectLogout)
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<Car | null>(null)
  const [showForm, setShowForm] = useState(false)

  const carsQuery = useQuery({
    queryKey: ['admin-cars'],
    queryFn: getAllCars,
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
        <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-100 text-rose-600 dark:bg-rose-500/15 dark:text-rose-400">
          <ShieldAlert className="h-7 w-7" />
        </span>
        <h1 className="mt-4 text-2xl font-extrabold tracking-tight">Acceso restringido</h1>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
          Solo los administradores pueden gestionar el catálogo de autos. Tu cuenta actual no tiene
          permisos de administración.
        </p>
        <button type="button" onClick={logout} className={`${BTN_SECONDARY} mt-6`}>
          <LogOut className="h-4 w-4" />
          Cambiar de cuenta (salir)
        </button>
        <p className="mt-3 text-xs text-slate-500 dark:text-slate-400">
          Después de salir, ingresá con una cuenta administrador (ej. admin@driveai.com).
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
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Publica, edita y elimina autos del catálogo.
            {carsQuery.data && ` ${carsQuery.data.length} unidades publicadas.`}
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            setEditing(null)
            setShowForm(true)
          }}
          className={BTN_PRIMARY}
        >
          <Plus className="h-4 w-4" />
          Nuevo auto
        </button>
      </div>

      {deleteMutation.isError && (
        <p className={`${ALERT_ERROR} mb-4`} role="alert">
          {(deleteMutation.error as ApiError).message}
        </p>
      )}

      {carsQuery.isPending && (
        <div className="flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400" aria-busy="true">
          <Loader2 className="h-4 w-4 animate-spin" />
          Cargando inventario…
        </div>
      )}

      {carsQuery.isError && (
        <p className={ALERT_ERROR} role="alert">
          {(carsQuery.error as ApiError).message}
        </p>
      )}

      {carsQuery.data && carsQuery.data.length === 0 && (
        <div className={`${PANEL} px-6 py-16 text-center`}>
          <p className="font-semibold text-slate-700 dark:text-slate-200">No hay autos publicados</p>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Usá «Nuevo auto» para cargar la primera unidad del catálogo.
          </p>
        </div>
      )}

      {carsQuery.data && carsQuery.data.length > 0 && (
        <div className={`${PANEL} overflow-hidden`}>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500 dark:border-slate-800 dark:bg-slate-800/60 dark:text-slate-400">
                <tr>
                  <th className="px-4 py-3 font-semibold">Auto</th>
                  <th className="px-4 py-3 font-semibold">Año</th>
                  <th className="px-4 py-3 font-semibold">Precio</th>
                  <th className="px-4 py-3 font-semibold">Stock</th>
                  <th className="px-4 py-3 text-right font-semibold">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {carsQuery.data.map((car) => (
                  <tr key={car.id} className="transition hover:bg-slate-50 dark:hover:bg-slate-800/40">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <CarImage
                          src={car.imageUrl}
                          alt={`${car.brand} ${car.model}`}
                          className="h-12 w-16 shrink-0 rounded-lg"
                          compact
                        />
                        <div>
                          <p className="font-semibold text-slate-900 dark:text-white">
                            {car.brand} {car.model}
                          </p>
                          <p className="text-xs text-slate-500 dark:text-slate-400">
                            {BODY_LABELS[car.bodyType]} · {FUEL_LABELS[car.fuelType]}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">{car.year}</td>
                    <td className="px-4 py-3 font-semibold text-slate-900 dark:text-white">
                      {formatMoney(car.price)}
                    </td>
                    <td className="px-4 py-3">
                      {car.stock > 0 ? (
                        <span
                          className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                            car.stock <= 2
                              ? 'bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300'
                              : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300'
                          }`}
                        >
                          {formatNumber(car.stock)}
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2 py-0.5 text-xs font-semibold text-rose-700 dark:bg-rose-500/15 dark:text-rose-300">
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
                          className={`${BTN_SECONDARY} rounded-lg px-3 py-1.5 text-xs`}
                        >
                          <Pencil className="h-3.5 w-3.5" />
                          Editar
                        </button>
                        <button
                          type="button"
                          disabled={deleteMutation.isPending}
                          onClick={() => handleDelete(car)}
                          className="inline-flex items-center gap-1.5 rounded-lg border border-rose-200 bg-rose-50 px-3 py-1.5 text-xs font-semibold text-rose-700 transition hover:bg-rose-100 disabled:opacity-50 dark:border-rose-500/40 dark:bg-rose-500/10 dark:text-rose-300 dark:hover:bg-rose-500/20"
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