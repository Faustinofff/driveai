import { useState } from 'react'
import {
  Car,
  Home,
  LayoutDashboard,
  LogIn,
  LogOut,
  Menu,
  Search,
  ShoppingCart,
  Sparkles,
  X,
} from 'lucide-react'
import { ThemeToggle } from './ThemeToggle'
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
  const [menuOpen, setMenuOpen] = useState(false)

  const go = (next: AppView) => {
    setMenuOpen(false)
    onViewChange(next)
  }
  const openAi = () => {
    setMenuOpen(false)
    if (isAuthenticated) {
      onViewChange('ai')
    } else {
      onLoginRequested('ai', 'Inicia sesión para consultar al asesor de IA.')
    }
  }
  const openCart = () => {
    setMenuOpen(false)
    if (isAuthenticated) {
      onOpenCart()
    } else {
      onLoginRequested('home', 'Inicia sesión para comprar en DriveAI.')
    }
  }
  const handleLogout = () => {
    setMenuOpen(false)
    logout()
    onViewChange('home')
  }

  const initial = user?.fullName.trim().charAt(0).toUpperCase() ?? '?'
  const showSearch = view === 'catalog' || view === 'home'

  const navLinkClass = (active: boolean) =>
    `inline-flex items-center gap-1.5 rounded-full px-3.5 py-2 text-sm font-medium transition ${
      active
        ? 'bg-white text-brand-700 shadow-sm dark:bg-slate-800 dark:text-brand-300'
        : 'text-slate-600 hover:text-slate-800 dark:text-slate-400 dark:hover:text-white'
    }`

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/90 backdrop-blur transition-colors duration-300 dark:border-slate-800 dark:bg-slate-950/85">
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-2 px-4 sm:gap-3 lg:px-8">
        <button
          type="button"
          onClick={() => go('home')}
          className="flex shrink-0 items-center gap-2 font-bold tracking-tight"
          aria-label="DriveAI - Ir al inicio"
        >
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-white shadow-md shadow-brand-600/30 transition hover:shadow-lg hover:shadow-brand-600/40">
            <Car className="h-5 w-5" />
          </span>
          <span className="hidden text-lg min-[420px]:inline">
            Drive<span className="text-brand-600 dark:text-brand-300">AI</span>
          </span>
        </button>

        <nav className="hidden shrink-0 items-center rounded-full border border-slate-200 bg-slate-100 p-1 dark:border-slate-800 dark:bg-slate-900 lg:flex">
          <button
            type="button"
            onClick={() => go('home')}
            className={navLinkClass(view === 'home')}
          >
            <Home className="h-4 w-4" />
            Inicio
          </button>
          <button
            type="button"
            onClick={() => go('catalog')}
            className={navLinkClass(view === 'catalog')}
          >
            <Car className="h-4 w-4" />
            Productos
          </button>
          <button
            type="button"
            onClick={openAi}
            className={navLinkClass(false)}
          >
            <Sparkles className="h-4 w-4" />
            IA
          </button>
          {isAuthenticated && isAdmin && (
            <button
              type="button"
              onClick={() => go('admin')}
              className={navLinkClass(view === 'admin')}
            >
              <LayoutDashboard className="h-4 w-4" />
              Panel
            </button>
          )}
        </nav>

        <div className="relative hidden min-w-0 flex-1 justify-center md:flex">
          {showSearch ? (
            <div className="relative w-full max-w-md">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                type="search"
                value={searchValue}
                onChange={(event) => onSearchChange(event.target.value)}
                placeholder="Buscar marca, modelo…"
                className="w-full rounded-full border border-slate-300 bg-white py-2 pl-9 pr-4 text-sm shadow-sm outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500"
              />
            </div>
          ) : (
            <span className="hidden text-sm text-slate-600 dark:text-slate-400 xl:block">
              E-commerce de autos certificados
            </span>
          )}
        </div>

        <div className="ml-auto flex shrink-0 items-center gap-1.5 sm:gap-2">
          <ThemeToggle />

          <button
            type="button"
            onClick={openCart}
            className="relative flex h-9 w-9 items-center justify-center rounded-full border border-slate-300 bg-white text-slate-700 shadow-sm transition hover:border-brand-400 hover:text-brand-700 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:border-brand-400 dark:hover:text-brand-300"
            aria-label="Abrir carrito"
          >
            <ShoppingCart className="h-[18px] w-[18px]" />
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
            <>
              <div className="hidden h-9 items-center gap-1 rounded-full border border-slate-200 bg-slate-100 pl-1.5 pr-1 dark:border-slate-800 dark:bg-slate-900 sm:flex">
                <span className="flex h-6 w-6 items-center justify-center rounded-full bg-brand-600 text-[11px] font-bold text-white">
                  {initial}
                </span>
                <span className="hidden max-w-24 truncate text-sm font-semibold text-slate-700 dark:text-slate-200 lg:block">
                  {user?.fullName.split(' ')[0]}
                </span>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="flex h-7 w-7 items-center justify-center rounded-full text-slate-500 transition hover:bg-white hover:text-rose-600 dark:text-slate-400 dark:hover:bg-slate-800"
                  aria-label="Cerrar sesión"
                  title="Cerrar sesión"
                >
                  <LogOut className="h-4 w-4" />
                </button>
              </div>
              <button
                type="button"
                onClick={handleLogout}
                className="flex h-9 w-9 items-center justify-center rounded-full border border-slate-300 bg-white text-slate-600 shadow-sm transition hover:border-rose-300 hover:text-rose-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 sm:hidden"
                aria-label="Cerrar sesión"
                title="Cerrar sesión"
              >
                <LogOut className="h-4 w-4" />
              </button>
            </>
          ) : (
            <button
              type="button"
              onClick={() => {
                setMenuOpen(false)
                onLoginRequested('home')
              }}
              className="hidden items-center gap-1.5 rounded-full bg-brand-600 px-4 py-2 text-sm font-semibold text-white shadow-md shadow-brand-600/20 transition hover:bg-brand-700 active:scale-[0.98] md:inline-flex"
            >
              <LogIn className="h-4 w-4" />
              Ingresar
            </button>
          )}

          <button
            type="button"
            onClick={() => setMenuOpen((open) => !open)}
            className="flex h-9 w-9 items-center justify-center rounded-full border border-slate-300 bg-white text-slate-700 shadow-sm transition hover:border-brand-400 hover:text-brand-700 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 lg:hidden"
            aria-label={menuOpen ? 'Cerrar menú' : 'Abrir menú'}
          >
            {menuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {showSearch && (
        <div className="px-4 pb-3 md:hidden">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="search"
              value={searchValue}
              onChange={(event) => onSearchChange(event.target.value)}
              placeholder="Buscar marca, modelo…"
              className="w-full rounded-full border border-slate-300 bg-white py-2 pl-9 pr-4 text-sm shadow-sm outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500"
            />
          </div>
        </div>
      )}

      {menuOpen && (
        <div className="border-t border-slate-200 bg-white px-4 pb-5 pt-3 shadow-xl animate-fade-in-up dark:border-slate-800 dark:bg-slate-950 lg:hidden">
          <nav className="flex flex-col gap-1">
            <button
              type="button"
              onClick={() => go('home')}
              className="flex items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-900"
            >
              <Home className="h-4 w-4 text-brand-600 dark:text-brand-400" />
              Inicio
            </button>
            <button
              type="button"
              onClick={() => go('catalog')}
              className="flex items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-900"
            >
              <Car className="h-4 w-4 text-brand-600 dark:text-brand-400" />
              Productos
            </button>
            <button
              type="button"
              onClick={openAi}
              className="flex items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-900"
            >
              <Sparkles className="h-4 w-4 text-brand-600 dark:text-brand-400" />
              Asesor IA
            </button>
            {isAuthenticated && isAdmin && (
              <button
                type="button"
                onClick={() => go('admin')}
                className="flex items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-900"
              >
                <LayoutDashboard className="h-4 w-4 text-brand-600 dark:text-brand-400" />
                Panel de administración
              </button>
            )}
            <div className="my-2 border-t border-slate-200 dark:border-slate-800" />
            {isAuthenticated ? (
              <div className="flex items-center justify-between px-3 py-2">
                <span className="flex items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-200">
                  <span className="flex h-7 w-7 items-center justify-center rounded-full bg-brand-600 text-[11px] font-bold text-white">
                    {initial}
                  </span>
                  {user?.fullName}
                </span>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-600 transition hover:border-rose-300 hover:text-rose-600 dark:border-slate-700 dark:text-slate-300"
                >
                  <LogOut className="h-3.5 w-3.5" />
                  Cerrar sesión
                </button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => {
                  setMenuOpen(false)
                  onLoginRequested('home')
                }}
                className="inline-flex items-center justify-center gap-1.5 rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white shadow-md shadow-brand-600/20 transition hover:bg-brand-700"
              >
                <LogIn className="h-4 w-4" />
                Ingresar
              </button>
            )}
          </nav>
        </div>
      )}
    </header>
  )
}
