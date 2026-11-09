import { useEffect, useState } from 'react'
import { AlertCircle, RefreshCw, SearchX } from 'lucide-react'
import { CarCard, CarCardSkeleton } from '../components/CarCard'
import { CarFiltersPanel } from '../components/CarFiltersPanel'
import { Pagination } from '../components/Pagination'
import { useCars } from '../hooks/useCars'
import type { CarFilters } from '../types'

interface CatalogPageProps {
  q: string
  onRequireLogin: () => void
  onClearSearch?: () => void
}

const SKELETON_COUNT = 8

function readFiltersFromURL(): CarFilters {
  const params = new URLSearchParams(window.location.search)
  const filters: CarFilters = {}
  const brand = params.get('brand')
  if (brand) filters.brand = brand
  const minPrice = params.get('minPrice')
  if (minPrice !== null && !Number.isNaN(Number(minPrice))) filters.minPrice = Number(minPrice)
  const maxPrice = params.get('maxPrice')
  if (maxPrice !== null && !Number.isNaN(Number(maxPrice))) filters.maxPrice = Number(maxPrice)
  const minYear = params.get('minYear')
  if (minYear !== null && !Number.isNaN(Number(minYear))) filters.minYear = Number(minYear)
  const fuelType = params.get('fuelType')
  if (fuelType) filters.fuelType = fuelType as CarFilters['fuelType']
  const bodyType = params.get('bodyType')
  if (bodyType) filters.bodyType = bodyType as CarFilters['bodyType']
  const q = params.get('q')
  if (q) filters.q = q
  return filters
}

function filtersToSearch(filters: CarFilters): string {
  const params = new URLSearchParams()
  if (filters.brand) params.set('brand', filters.brand)
  if (filters.minPrice !== undefined) params.set('minPrice', String(filters.minPrice))
  if (filters.maxPrice !== undefined) params.set('maxPrice', String(filters.maxPrice))
  if (filters.minYear !== undefined) params.set('minYear', String(filters.minYear))
  if (filters.fuelType) params.set('fuelType', filters.fuelType)
  if (filters.bodyType) params.set('bodyType', filters.bodyType)
  if (filters.q) params.set('q', filters.q)
  const value = params.toString()
  return value ? `?${value}` : ''
}

export function CatalogPage({ q, onRequireLogin, onClearSearch }: CatalogPageProps) {
  const [filters, setFilters] = useState<CarFilters>(() => readFiltersFromURL())
  const [page, setPage] = useState(0)

  useEffect(() => {
    setFilters((current) => ({ ...current, q }))
    setPage(0)
  }, [q])

  useEffect(() => {
    // Se preserva el hash (#/catalog) porque la SPA navega por hash: si se
    // reemplazara solo con "?filtros" se perdería la vista al recargar.
    window.history.replaceState(
      null,
      '',
      `${window.location.pathname}${filtersToSearch(filters)}${window.location.hash}`,
    )
  }, [filters])

  const updateFilters = (next: CarFilters) => {
    setFilters(next)
    setPage(0)
  }

  const clearFilters = () => {
    setFilters({})
    setPage(0)
    // La búsqueda del navbar es parte del filtrado: limpiar filtros también
    // debe vaciar el texto para no dejar resultados ocultos por la query.
    onClearSearch?.()
  }

  const query = useCars({ ...filters, q }, page)
  const isLoading = query.isPending
  const isError = query.isError && query.data === undefined
  const isEmpty = query.isSuccess && query.data.content.length === 0

  return (
    <div className="mx-auto max-w-7xl px-4 py-6">
      <div className="mb-6 flex flex-col gap-2">
        <h1 className="text-2xl font-extrabold tracking-tight">
          Catálogo de autos certificados
        </h1>
        <p className="text-sm text-slate-500">
          {query.data
            ? `${query.data.totalElements} vehículos disponibles en stock`
            : 'Explora nuestras unidades disponibles'}
        </p>
      </div>

      <div className="flex flex-col gap-6 lg:flex-row">
        <aside className="shrink-0 lg:w-72">
          <CarFiltersPanel filters={filters} onChange={updateFilters} onClear={clearFilters} />
        </aside>

        <section className="flex-1">
          {isLoading && (
            <div className="grid gap-6 sm:grid-cols-2 xl:grid-cols-3">
              {Array.from({ length: SKELETON_COUNT }, (_, index) => (
                <CarCardSkeleton key={index} />
              ))}
            </div>
          )}

          {isError && (
            <div className="flex flex-col items-center justify-center rounded-2xl border border-rose-200 bg-rose-50 px-6 py-16 text-center">
              <AlertCircle className="h-10 w-10 text-rose-500" />
              <p className="mt-3 font-semibold text-rose-700">No se pudo cargar el catálogo</p>
              <p className="mt-1 text-sm text-rose-500">
                Verifica que el backend esté disponible e intenta de nuevo.
              </p>
              <button
                type="button"
                onClick={() => void query.refetch()}
                className="mt-5 inline-flex items-center gap-2 rounded-xl bg-rose-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-rose-700"
              >
                <RefreshCw className="h-4 w-4" />
                Reintentar
              </button>
            </div>
          )}

          {isEmpty && (
            <div className="flex flex-col items-center justify-center rounded-2xl border border-slate-200 bg-white px-6 py-16 text-center">
              <SearchX className="h-10 w-10 text-slate-400" />
              <p className="mt-3 font-semibold text-slate-700">Sin resultados</p>
              <p className="mt-1 max-w-sm text-sm text-slate-500">
                Ningún auto coincide con los filtros aplicados.
              </p>
              <button
                type="button"
                onClick={clearFilters}
                className="mt-5 rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-brand-700"
              >
                Limpiar filtros
              </button>
            </div>
          )}

          {query.data && !isLoading && !isEmpty && (
            <div className="grid gap-6 sm:grid-cols-2 xl:grid-cols-3">
              {query.data.content.map((car) => (
                <CarCard key={car.id} car={car} onRequireLogin={onRequireLogin} />
              ))}
            </div>
          )}

          {query.data && !isEmpty && (
            <Pagination
              page={page}
              totalPages={query.data.totalPages}
              onPageChange={setPage}
            />
          )}
        </section>
      </div>
    </div>
  )
}