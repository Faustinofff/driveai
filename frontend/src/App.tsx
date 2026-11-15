import { useCallback, useEffect, useRef, useState } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { CartDrawer } from './components/CartDrawer'
import { CheckoutModal } from './components/CheckoutModal'
import { Footer } from './components/Footer'
import { Navbar } from './components/Navbar'
import { useDebounce } from './hooks/useDebounce'
import { AdminPage } from './pages/AdminPage'
import { AiConciergePage } from './pages/AiConciergePage'
import { CatalogPage } from './pages/CatalogPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { OrdersPage } from './pages/OrdersPage'
import { selectAuthenticated, useAuthStore } from './store/authStore'
import type { BodyType, Order } from './types'
import { LOGIN_NOTICES, PROTECTED_VIEWS, type AppView } from './types/view'

const VALID_VIEWS: AppView[] = ['home', 'catalog', 'ai', 'orders', 'admin', 'login']

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

function readViewFromHash(): AppView | null {
  const hash = window.location.hash.replace(/^#\/?/, '') as AppView
  return VALID_VIEWS.includes(hash) ? hash : null
}

export default function App() {
  const [view, setView] = useState<AppView>(() => readViewFromHash() ?? 'home')
  const [searchValue, setSearchValue] = useState('')
  const debouncedSearch = useDebounce(searchValue, 400)
  const [cartOpen, setCartOpen] = useState(false)
  const [order, setOrder] = useState<Order | null>(null)
  const [loginDestination, setLoginDestination] = useState<AppView>('home')
  const [loginNotice, setLoginNotice] = useState<string | undefined>(undefined)

  const isAuthenticated = useAuthStore(selectAuthenticated)
  // Una redirección al login reemplaza la entrada de historial en vez de
  // apilarla: si no, "Atrás" volvía a la vista protegida y rebotaba otra vez.
  const replaceNextHash = useRef(false)

  const openLogin = useCallback((destination: AppView, notice?: string) => {
    setLoginDestination(destination)
    setLoginNotice(notice)
    if (PROTECTED_VIEWS.includes(destination)) replaceNextHash.current = true
    setView('login')
  }, [])

  const resolveView = useCallback(
    (next: AppView) => {
      if (!isAuthenticated && PROTECTED_VIEWS.includes(next)) {
        openLogin(next, LOGIN_NOTICES[next])
      } else {
        setView(next)
      }
    },
    [isAuthenticated, openLogin],
  )

  // Vistas protegidas: sin sesión se redirige al login con un aviso claro
  // (cubre también el cierre de sesión estando en una de ellas).
  useEffect(() => {
    if (!isAuthenticated && PROTECTED_VIEWS.includes(view)) {
      openLogin(view, LOGIN_NOTICES[view])
    }
  }, [view, isAuthenticated, openLogin])

  // La URL es la fuente de verdad al cambiar el hash y al usar atrás/adelante
  // del navegador (popstate cubre las entradas creadas con pushState). El
  // estado inicial ya se lee del hash en useState, así que acá solo se escucha.
  useEffect(() => {
    const applyHash = () => {
      const next = readViewFromHash()
      if (next) resolveView(next)
    }
    window.addEventListener('hashchange', applyHash)
    window.addEventListener('popstate', applyHash)
    return () => {
      window.removeEventListener('hashchange', applyHash)
      window.removeEventListener('popstate', applyHash)
    }
  }, [resolveView])

  // Y al revés: cada cambio de vista queda reflejado en el hash, así recargar
  // la página o compartir el enlace conserva la pantalla actual. Fuera del
  // catálogo se descartan los filtros (?brand=…) para no arrastrarlos.
  useEffect(() => {
    const target = `#/${view}`
    const replace = replaceNextHash.current
    replaceNextHash.current = false
    if (window.location.hash === target) return
    const search = view === 'catalog' ? window.location.search : ''
    const url = `${window.location.pathname}${search}${target}`
    if (replace || window.location.hash === '') {
      window.history.replaceState(null, '', url)
    } else {
      window.history.pushState(null, '', url)
    }
  }, [view])

  // Escribir en el buscador desde el inicio lleva al catálogo, que es donde
  // el texto filtra resultados. Antes el campo no hacía nada en esa vista.
  useEffect(() => {
    if (view === 'home' && searchValue.trim() !== '') {
      setView('catalog')
    }
  }, [searchValue, view])

  const openCatalogWithCategory = (bodyType: BodyType) => {
    window.history.pushState(
      null,
      '',
      `${window.location.pathname}?bodyType=${bodyType}#/catalog`,
    )
    setView('catalog')
    setSearchValue('')
  }

  const showOrders = () => {
    setOrder(null)
    setView('orders')
  }

  return (
    <QueryClientProvider client={queryClient}>
      <div className="flex min-h-screen flex-col">
        <Navbar
          view={view}
          onViewChange={resolveView}
          searchValue={searchValue}
          onSearchChange={setSearchValue}
          onOpenCart={() => setCartOpen(true)}
          onLoginRequested={openLogin}
        />

        <main className="flex-1">
          {view === 'home' && (
            <HomePage
              onNavigate={resolveView}
              onRequireLogin={() => openLogin('home', 'Inicia sesión para comprar en DriveAI.')}
              onSelectCategory={openCatalogWithCategory}
            />
          )}
          {view === 'catalog' && (
            <CatalogPage
              q={debouncedSearch}
              onRequireLogin={() => openLogin('catalog', 'Inicia sesión para comprar en DriveAI.')}
              onClearSearch={() => setSearchValue('')}
            />
          )}
          {view === 'ai' && isAuthenticated && <AiConciergePage />}
          {view === 'orders' && isAuthenticated && <OrdersPage onNavigate={setView} />}
          {view === 'admin' && isAuthenticated && <AdminPage />}
          {view === 'login' && (
            <LoginPage
              notice={loginNotice}
              onSuccess={() => setView(loginDestination)}
            />
          )}
        </main>

        <Footer onNavigate={resolveView} />

        <CartDrawer
          open={cartOpen}
          onClose={() => setCartOpen(false)}
          onOrderCreated={(created) => setOrder(created)}
        />
        <CheckoutModal order={order} onClose={() => setOrder(null)} onViewOrders={showOrders} />
      </div>
    </QueryClientProvider>
  )
}
