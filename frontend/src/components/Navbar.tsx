import { Car, Search, ShoppingCart, Sparkles } from 'lucide-react'
import { useCartStore } from '../store/cartStore'
import type { AppView } from '../types/view'

interface NavbarProps {
  view: AppView
  onViewChange: (view: AppView) => void
  searchValue: string
  onSearchChange: (value: string) => void
  onOpenCart: () => void
}

export function Navbar({ view, onViewChange, searchValue, onSearchChange, onOpenCart }: NavbarProps) {
  const count = useCartStore((state) =>
    state.lines.reduce((total, line) => total + line.quantity, 0),
  )

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-3 px-4 sm:gap-5">
        <a href="/" className="flex items-center gap-2 font-bold tracking-tight" onClick={(e) => e.preventDefault()}>
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-white shadow-md shadow-brand-600/30">
            <Car className="h-5 w-5" />
          </span>
          <span className="text-lg sm:text-xl">
            Drive<span className="text-brand-600">AI</span>
          </span>
        </a>

        <div className="hidden items-center rounded-full border border-slate-200 bg-slate-100 p-1 sm:flex">
          <button
            type="button"
            onClick={() => onViewChange('catalog')}
            className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-medium transition ${
              view === 'catalog'
                ? 'bg-white text-brand-700 shadow-sm'
                : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            <Car className="h-4 w-4" />
            Catálogo
          </button>
          <button
            type="button"
            onClick={() => onViewChange('ai')}
            className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-medium transition ${
              view === 'ai' ? 'bg-white text-brand-700 shadow-sm' : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            <Sparkles className="h-4 w-4" />
            Asesor IA
          </button>
        </div>

        <div className="relative flex-1 sm:flex-none">
          {view === 'catalog' ? (
            <div className="relative mx-auto w-full max-w-md">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                type="search"
                value={searchValue}
                onChange={(event) => onSearchChange(event.target.value)}
                placeholder="Buscar marca, modelo…"
                className="w-full rounded-full border border-slate-300 bg-white py-2 pl-9 pr-4 text-sm shadow-sm outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
              />
            </div>
          ) : (
            <div className="hidden shrink-0 text-sm text-slate-400 sm:block">
              Asesor de compra inteligente
            </div>
          )}
        </div>

        <div className="ml-auto flex items-center gap-2 sm:ml-0">
          <button
            type="button"
            onClick={() => onViewChange('ai')}
            className={`flex items-center rounded-full border px-2.5 py-2 text-sm font-medium transition sm:hidden ${
              view === 'ai'
                ? 'border-brand-500 bg-brand-50 text-brand-700'
                : 'border-slate-300 text-slate-600 hover:border-brand-400'
            }`}
            aria-label="Asesor IA"
          >
            <Sparkles className="h-4 w-4" />
          </button>

          <button
            type="button"
            onClick={onOpenCart}
            className="relative flex h-10 w-10 items-center justify-center rounded-full border border-slate-300 bg-white text-slate-700 shadow-sm transition hover:border-brand-400 hover:text-brand-700"
            aria-label="Abrir carrito"
          >
            <ShoppingCart className="h-5 w-5" />
            {count > 0 && (
              <span
                key={count}
                className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-brand-600 px-1 text-xs font-bold text-white shadow animate-pop"
              >
                {count}
              </span>
            )}
          </button>
        </div>
      </div>
    </header>
  )
}