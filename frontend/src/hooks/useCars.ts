import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getCars } from '../services/api'
import type { CarFilters } from '../types'

export interface CarsQuery extends CarFilters {
  page: number
}

export function useCars(filters: CarFilters, page: number) {
  return useQuery({
    queryKey: ['cars', filters, page],
    queryFn: () => getCars({ ...filters, page }),
    placeholderData: keepPreviousData,
    staleTime: 60_000,
  })
}