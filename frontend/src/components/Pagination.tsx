import { ChevronLeft, ChevronRight } from 'lucide-react'

interface PaginationProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

function visiblePages(current: number, total: number): number[] {
  if (total <= 7) {
    return Array.from({ length: total }, (_, index) => index)
  }
  const start = Math.max(0, Math.min(current - 2, total - 5))
  return [0, ...Array.from({ length: 5 }, (_, index) => start + index), total - 1].filter(
    (value, index, array) => array.indexOf(value) === index && value >= 0 && value < total,
  )
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null
  const pages = visiblePages(page, totalPages)

  return (
    <nav className="mt-8 flex items-center justify-center gap-1.5" aria-label="Paginación">
      <button
        type="button"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
        className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-300 bg-white text-slate-600 transition hover:border-brand-400 hover:text-brand-700 disabled:cursor-not-allowed disabled:opacity-40"
        aria-label="Página anterior"
      >
        <ChevronLeft className="h-4 w-4" />
      </button>

      {pages.map((number) => (
        <button
          key={number}
          type="button"
          onClick={() => onPageChange(number)}
          aria-current={number === page ? 'page' : undefined}
          className={`inline-flex h-9 min-w-9 items-center justify-center rounded-lg px-2 text-sm font-semibold transition ${
            number === page
              ? 'bg-brand-600 text-white shadow-md shadow-brand-600/20'
              : 'border border-slate-300 bg-white text-slate-600 hover:border-brand-400 hover:text-brand-700'
          }`}
        >
          {number + 1}
        </button>
      ))}

      <button
        type="button"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
        className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-300 bg-white text-slate-600 transition hover:border-brand-400 hover:text-brand-700 disabled:cursor-not-allowed disabled:opacity-40"
        aria-label="Página siguiente"
      >
        <ChevronRight className="h-4 w-4" />
      </button>
    </nav>
  )
}