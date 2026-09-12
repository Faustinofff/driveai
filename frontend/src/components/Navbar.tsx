import { Car, LayoutDashboard, LogIn, LogOut, Search, ShoppingCart, Sparkles } from 'lucide-react'
import { selectIsAdmin, selectLogout, selectUser, useAuthStore } from '../store/authStore'
import { useCartStore } from '../store/cartStore'
import type { AppView } from '../types/view'

interface NavbarProps {
  view: AppView
  onViewChange: (view: AppView) => void
  searchValue: string
  onSearchChange: (value: string) => void
  onOpenCart: () => void
  onLoginRequested: (destination: AppView, notice?: string) => void
}

export function Navbar({
  view,
  onViewChange,
  searchValue,
  onSearchChange,
  onOpenCart,
  onLoginRequested,
}: NavbarProps) {
  const isAuthenticated = useAuthStore((state) => state.token !== null && state.user !== null)
  const isAdmin = useAuthStore(selectIsAdmin)
  const user = useAuthStore(selectUser)
  const logout = useAuthStore(selectLogout)
  const count = useCartStore((state) =>
    state.lines.reduce((total, line) => total + line.quantity, 0),
  )

  const openCatalogView = () => onViewChange('catalog')
  const openAiView = () => (isAuthenticated ? onViewChange('ai') : onLoginRequested('ai', 'Inicia sesión para consultar al asesor de IA.'))
  const openCart = () => (isAuthenticated ? onOpenCart() : onLoginRequested('catalog', 'Inicia sesión para comprar en DriveAI.'))
  const handleLogout = () => {
    logout()
    onViewChange('catalog')
  }

  const initial = user?.fullName.trim().charAt(0).toUpperCase() ?? '?'

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
            onClick={openCatalogView}
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
            onClick={openAiView}
            className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-medium transition ${
              view === 'ai' ? 'bg-white text-brand-700 shadow-sm' : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            <Sparkles className="h-4 w-4" />
            Asesor IA
          </button>
          {isAuthenticated && isAdmin && (
            <button
              type="button"
              onClick={() => onViewChange('admin')}
              className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-medium transition ${
                view === 'admin' ? 'bg-white text-brand-700 shadow-sm' : 'text-slate-500 hover:text-slate-700'
              }`}
            >
              <LayoutDashboard className="h-4 w-4" />
              Panel
            </button>
          )}
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
            onClick={openAiView}
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
            onClick={openCart}
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

          {isAuthenticated ? (
            <div className="flex h-10 items-center gap-1 rounded-full border border-slate-200 bg-slate-100 pl-1.5 pr-1">
              <span className="flex h-7 w-7 items-center justify-center rounded-full bg-brand-600 text-xs font-bold text-white">
                {initial}
              </span>
              <span className="hidden max-w-28 truncate text-sm font-semibold text-slate-700 lg:block">
                {user?.fullName.split(' ')[0]}
              </span>
              <button
                type="button"
                onClick={handleLogout}
                className="flex h-8 w-8 items-center justify-center rounded-full text-slate-500 transition hover:bg-white hover:text-rose-600"
                aria-label="Cerrar sesión"
                title="Cerrar sesión"
              >
                <LogOut className="h-4 w-4" />
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => onLoginRequested('catalog')}
              className="flex items-center gap-1.5 rounded-full bg-brand-600 px-4 py-2 text-sm font-semibold text-white shadow-md shadow-brand-600/20 transition hover:bg-brand-700"
            >
              <LogIn className="h-4 w-4" />
              <span className="hidden sm:inline">Ingresar</span>
            </button>
          )}
        </div>
      </div>
    </header>
  )
}