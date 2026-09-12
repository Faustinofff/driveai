import { useEffect, useState } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Navbar } from './components/Navbar'
import { CartDrawer } from './components/CartDrawer'
import { CheckoutModal } from './components/CheckoutModal'
import { useDebounce } from './hooks/useDebounce'
import { AdminPage } from './pages/AdminPage'
import { AiConciergePage } from './pages/AiConciergePage'
import { CatalogPage } from './pages/CatalogPage'
import { LoginPage } from './pages/LoginPage'
import { selectAuthenticated, useAuthStore } from './store/authStore'
import type { Order } from './types'
import type { AppView } from './types/view'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

export default function App() {
  const [view, setView] = useState<AppView>('catalog')
  const [searchValue, setSearchValue] = useState('')
  const debouncedSearch = useDebounce(searchValue, 400)
  const [cartOpen, setCartOpen] = useState(false)
  const [order, setOrder] = useState<Order | null>(null)
  const [loginDestination, setLoginDestination] = useState<AppView>('catalog')
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

  const openLogin = (destination: AppView, notice?: string) => {
    setLoginDestination(destination)
    setLoginNotice(notice)
    setView('login')
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

        <footer className="border-t border-slate-200 bg-white py-6">
          <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-2 px-4 text-sm text-slate-500 sm:flex-row">
            <span className="font-semibold text-slate-700">
              Drive<span className="text-brand-600">AI</span>
            </span>
            <span>E-commerce de autos con asesor inteligente (Groq Cloud).</span>
          </div>
        </footer>

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