import { useEffect, useState } from 'react'
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
import { selectAuthenticated, useAuthStore } from './store/authStore'
import type { BodyType, Order } from './types'
import type { AppView } from './types/view'

const VALID_VIEWS: AppView[] = ['home', 'catalog', 'ai', 'admin', 'login']

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

export default function App() {
  const [view, setView] = useState<AppView>('home')
  const [searchValue, setSearchValue] = useState('')
  const debouncedSearch = useDebounce(searchValue, 400)
  const [cartOpen, setCartOpen] = useState(false)
  const [order, setOrder] = useState<Order | null>(null)
  const [loginDestination, setLoginDestination] = useState<AppView>('home')
  const [loginNotice, setLoginNotice] = useState<string | undefined>(undefined)

  const isAuthenticated = useAuthStore(selectAuthenticated)

  useEffect(() => {
    if (!isAuthenticated && (view === 'ai' || view === 'admin')) {
      setLoginDestination(view)
      setLoginNotice(
        view === 'admin'
          ? 'Inicia sesión para entrar al panel de administración.'
          : 'Inicia sesión para consultar al asesor de IA.',
      )
      setView('login')
    }
  }, [view, isAuthenticated])

  useEffect(() => {
    const applyHash = () => {
      const hash = window.location.hash.replace(/^#\/?/, '') as AppView
      if (!VALID_VIEWS.includes(hash)) return
      if (hash === 'admin') {
        if (!isAuthenticated) {
          setLoginDestination('admin')
          setLoginNotice('Inicia sesión para entrar al panel de administración.')
          setView('login')
        } else {
          setView('admin')
        }
        return
      }
      setView(hash)
    }
    applyHash()
    window.addEventListener('hashchange', applyHash)
    return () => window.removeEventListener('hashchange', applyHash)
  }, [isAuthenticated])

  const openLogin = (destination: AppView, notice?: string) => {
    setLoginDestination(destination)
    setLoginNotice(notice)
    setView('login')
  }

  const openCatalogWithCategory = (bodyType: BodyType) => {
    window.history.pushState(null, '', `?bodyType=${bodyType}`)
    setView('catalog')
    setSearchValue('')
  }

  return (
    <QueryClientProvider client={queryClient}>
      <div className="flex min-h-screen flex-col">
        <Navbar
          view={view}
          onViewChange={setView}
          searchValue={searchValue}
          onSearchChange={setSearchValue}
          onOpenCart={() => setCartOpen(true)}
          onLoginRequested={openLogin}
        />

        <main className="flex-1">
          {view === 'home' && (
            <HomePage
              onNavigate={setView}
              onRequireLogin={() => openLogin('home', 'Inicia sesión para comprar en DriveAI.')}
              onSelectCategory={openCatalogWithCategory}
            />
          )}
          {view === 'catalog' && (
            <CatalogPage
              q={debouncedSearch}
              onRequireLogin={() => openLogin('catalog', 'Inicia sesión para comprar en DriveAI.')}
            />
          )}
          {view === 'ai' && <AiConciergePage />}
          {view === 'admin' && <AdminPage />}
          {view === 'login' && (
            <LoginPage
              notice={loginNotice}
              onSuccess={() => setView(loginDestination)}
            />
          )}
        </main>

        <Footer onNavigate={setView} />

        <CartDrawer
          open={cartOpen}
          onClose={() => setCartOpen(false)}
          onOrderCreated={(created) => setOrder(created)}
        />
        <CheckoutModal order={order} onClose={() => setOrder(null)} />
      </div>
    </QueryClientProvider>
  )
}