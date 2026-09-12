import { useQuery } from '@tanstack/react-query'
import {
  ArrowRight,
  Car,
  Gauge,
  Leaf,
  ShieldCheck,
  Sparkles,
  Truck,
  Wallet,
} from 'lucide-react'
import { CarCard, CarCardSkeleton } from '../components/CarCard'
import { HeroCarousel } from '../components/HeroCarousel'
import { Reveal } from '../components/Reveal'
import { getCars } from '../services/api'
import type { BodyType } from '../types'
import type { AppView } from '../types/view'

interface HomePageProps {
  onNavigate: (view: AppView) => void
  onRequireLogin: () => void
  onSelectCategory: (bodyType: BodyType) => void
}

const CATEGORIES: { bodyType: BodyType; label: string; description: string; Icon: typeof Car }[] = [
  { bodyType: 'SUV', label: 'SUVs', description: 'Espacio y versatilidad para la familia', Icon: Car },
  { bodyType: 'SEDAN', label: 'Sedanes', description: 'Confort y eficiencia para la ciudad', Icon: Car },
  { bodyType: 'HATCHBACK', label: 'Hatchbacks', description: 'Ágiles, económicos y compactos', Icon: Gauge },
  { bodyType: 'PICKUP', label: 'Pickups', description: 'Trabajo y aventura sin límites', Icon: Truck },
]

const FEATURES = [
  { Icon: ShieldCheck, title: 'Unidades verificadas', text: 'Cada auto del catálogo tiene ficha completa, historial y stock real.' },
  { Icon: Wallet, title: 'Precios transparentes', text: 'Sin sorpresas: el precio publicado es el final, con datos de consumo y kilometraje.' },
  { Icon: Leaf, title: 'Opción eléctrica e híbrida', text: 'Filtrá por combustible y elegí la alternativa más eficiente para vos.' },
]

export function HomePage({ onNavigate, onRequireLogin, onSelectCategory }: HomePageProps) {
  const featured = useQuery({
    queryKey: ['home-featured'],
    queryFn: () => getCars({ page: 0, size: 6 }),
  })

  return (
    <div className="overflow-x-hidden">
      <HeroCarousel onNavigate={onNavigate} />

      <section className="mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8">
        <Reveal>
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div>
              <p className="text-xs font-bold uppercase tracking-widest text-brand-600 dark:text-brand-400">
                Explora por categoría
              </p>
              <h2 className="mt-1 text-2xl font-extrabold tracking-tight text-slate-900 dark:text-white sm:text-3xl">
                ¿Qué tipo de auto buscás?
              </h2>
            </div>
            <button
              type="button"
              onClick={() => onNavigate('catalog')}
              className="inline-flex items-center gap-1.5 text-sm font-semibold text-brand-700 transition hover:gap-2.5 dark:text-brand-300"
            >
              Ver catálogo completo
              <ArrowRight className="h-4 w-4" />
            </button>
          </div>
        </Reveal>

        <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {CATEGORIES.map((category, index) => (
            <Reveal key={category.bodyType} delay={index * 70}>
              <button
                type="button"
                onClick={() => onSelectCategory(category.bodyType)}
                className="group relative w-full overflow-hidden rounded-2xl border border-slate-200 bg-white p-5 text-left shadow-sm transition hover:-translate-y-1 hover:border-brand-400 hover:shadow-lg dark:border-slate-800 dark:bg-slate-900 dark:hover:border-brand-500"
              >
                <div className="absolute -right-6 -top-6 h-24 w-24 rounded-full bg-brand-600/10 transition-transform duration-500 group-hover:scale-150" />
                <div className="relative">
                  <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-600/10 text-brand-700 transition group-hover:bg-brand-600 group-hover:text-white dark:bg-brand-500/15 dark:text-brand-300 dark:group-hover:bg-brand-600 dark:group-hover:text-white">
                    <category.Icon className="h-5 w-5" />
                  </span>
                  <h3 className="mt-4 text-lg font-bold text-slate-900 dark:text-white">
                    {category.label}
                  </h3>
                  <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                    {category.description}
                  </p>
                  <span className="mt-3 inline-flex items-center gap-1 text-xs font-semibold text-brand-700 transition group-hover:gap-2 dark:text-brand-300">
                    Explorar
                    <ArrowRight className="h-3.5 w-3.5" />
                  </span>
                </div>
              </button>
            </Reveal>
          ))}
        </div>
      </section>

      <section className="border-y border-slate-200 bg-white transition-colors duration-300 dark:border-slate-800 dark:bg-slate-900/40">
        <div className="mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8">
          <Reveal>
            <div className="flex flex-wrap items-end justify-between gap-3">
              <div>
                <p className="text-xs font-bold uppercase tracking-widest text-brand-600 dark:text-brand-400">
                  En stock ahora mismo
                </p>
                <h2 className="mt-1 text-2xl font-extrabold tracking-tight text-slate-900 dark:text-white sm:text-3xl">
                  Destacados del catálogo
                </h2>
              </div>
              <button
                type="button"
                onClick={() => onNavigate('catalog')}
                className="inline-flex items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-brand-400 hover:text-brand-700 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:border-brand-400 dark:hover:text-brand-300"
              >
                Ver todos
                <ArrowRight className="h-4 w-4" />
              </button>
            </div>
          </Reveal>

          <div className="mt-8 grid gap-6 sm:grid-cols-2 xl:grid-cols-3">
            {featured.isPending &&
              Array.from({ length: 6 }, (_, index) => <CarCardSkeleton key={index} />)}
            {featured.data?.content.map((car, index) => (
              <Reveal key={car.id} delay={index * 60}>
                <CarCard car={car} onRequireLogin={onRequireLogin} />
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8">
        <Reveal>
          <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-brand-600 via-brand-600 to-brand-800 px-6 py-12 text-center shadow-2xl shadow-brand-600/25 sm:px-12">
            <div className="pointer-events-none absolute -left-16 -top-16 h-56 w-56 rounded-full bg-white/10 blur-2xl" />
            <div className="pointer-events-none absolute -bottom-20 -right-16 h-64 w-64 rounded-full bg-white/10 blur-2xl" />
            <div className="relative mx-auto max-w-2xl">
              <span className="inline-flex items-center gap-2 rounded-full border border-white/25 bg-white/10 px-3 py-1 text-xs font-semibold uppercase tracking-widest text-white/90 backdrop-blur">
                <Sparkles className="h-3.5 w-3.5" />
                Asesor con IA
              </span>
              <h2 className="mt-4 text-balance text-2xl font-extrabold tracking-tight text-white sm:text-4xl">
                ¿No sabés qué auto elegir?
              </h2>
              <p className="mx-auto mt-3 max-w-xl text-sm leading-relaxed text-white/85 sm:text-base">
                Contale a nuestro asistente inteligente qué buscás (precio, uso, combustible) y
                recibe recomendaciones de unidades reales de nuestro inventario, al instante.
              </p>
              <button
                type="button"
                onClick={() => onNavigate('ai')}
                className="mt-6 inline-flex items-center gap-2 rounded-xl bg-white px-6 py-3 text-sm font-bold text-brand-700 shadow-lg transition hover:bg-brand-50 active:scale-[0.98]"
              >
                Probar el asesor
                <ArrowRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        </Reveal>
      </section>

      <section className="border-t border-slate-200 bg-white transition-colors duration-300 dark:border-slate-800 dark:bg-slate-900/40">
        <div className="mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8">
          <div className="grid items-center gap-10 lg:grid-cols-2">
            <Reveal>
              <div>
                <p className="text-xs font-bold uppercase tracking-widest text-brand-600 dark:text-brand-400">
                  Sobre nosotros
                </p>
                <h2 className="mt-1 text-2xl font-extrabold tracking-tight text-slate-900 dark:text-white sm:text-3xl">
                  Comprar un auto, sin vueltas
                </h2>
                <p className="mt-4 text-sm leading-relaxed text-slate-600 dark:text-slate-300 sm:text-base">
                  DriveAI es un e-commerce de autos certificados que une un catálogo curado y
                  transparente con un asistente de compra basado en inteligencia artificial. La
                  idea es simple: que encontrar tu próximo auto deje de ser una tarea pesada y se
                  convierta en una decisión informada y rápida.
                </p>
                <div className="mt-6 flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => onNavigate('catalog')}
                    className="inline-flex items-center gap-2 rounded-xl bg-brand-600 px-5 py-2.5 text-sm font-semibold text-white shadow-md shadow-brand-600/20 transition hover:bg-brand-700 active:scale-[0.98]"
                  >
                    Ir al catálogo
                    <ArrowRight className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onNavigate('ai')}
                    className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-brand-400 hover:text-brand-700 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:border-brand-400 dark:hover:text-brand-300"
                  >
                    <Sparkles className="h-4 w-4" />
                    Preguntale a la IA
                  </button>
                </div>
              </div>
            </Reveal>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3 lg:grid-cols-1 xl:grid-cols-3">
              {FEATURES.map((feature, index) => (
                <Reveal key={feature.title} delay={index * 80}>
                  <div className="h-full rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md dark:border-slate-800 dark:bg-slate-900">
                    <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-600/10 text-brand-700 dark:bg-brand-500/15 dark:text-brand-300">
                      <feature.Icon className="h-5 w-5" />
                    </span>
                    <h3 className="mt-3 text-sm font-bold text-slate-900 dark:text-white">
                      {feature.title}
                    </h3>
                    <p className="mt-1 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
                      {feature.text}
                    </p>
                  </div>
                </Reveal>
              ))}
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}