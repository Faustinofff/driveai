import { useState } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Navbar } from './components/Navbar'
import { CartDrawer } from './components/CartDrawer'
import { CheckoutModal } from './components/CheckoutModal'
import { useDebounce } from './hooks/useDebounce'
import { AiConciergePage } from './pages/AiConciergePage'
import { CatalogPage } from './pages/CatalogPage'
import type { AppView } from './types/view'
import type { Order } from './types'

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

  return (
    <QueryClientProvider client={queryClient}>
      <div className="flex min-h-screen flex-col">
        <Navbar
          view={view}
          onViewChange={setView}
          searchValue={searchValue}
          onSearchChange={setSearchValue}
          onOpenCart={() => setCartOpen(true)}
        />

        <main className="flex-1">
          {view === 'catalog' ? (
            <CatalogPage q={debouncedSearch} />
          ) : (
            <AiConciergePage />
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