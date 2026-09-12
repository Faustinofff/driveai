import { useEffect, useRef, useState } from 'react'
import { ArrowRight, ChevronLeft, ChevronRight, Sparkles } from 'lucide-react'
import type { AppView } from '../types/view'

export interface HeroSlide {
  id: string
  image: string
  eyebrow: string
  title: string
  subtitle: string
  ctaLabel: string
  ctaTarget: AppView
  secondary?: boolean
}

const SLIDES: HeroSlide[] = [
  {
    id: 'hero-1',
    image:
      'https://images.unsplash.com/photo-1606016159991-dfe4f2746ad5?auto=format&fit=crop&w=1800&q=85',
    eyebrow: 'Unidades certificadas',
    title: 'Tu próximo auto te está esperando',
    subtitle:
      'Explorá un catálogo curado de vehículos verificados, con precio, ficha técnica y stock real.',
    ctaLabel: 'Explorar catálogo',
    ctaTarget: 'catalog',
  },
  {
    id: 'hero-2',
    image:
      'https://images.unsplash.com/photo-1553440569-bcc63803a83d?auto=format&fit=crop&w=1800&q=85',
    eyebrow: 'Asesor inteligente',
    title: '¿No sabés qué auto elegir?',
    subtitle:
      'Nuestro asistente con IA analiza el inventario real y te recomienda el vehículo ideal para vos.',
    ctaLabel: 'Probar el asesor IA',
    ctaTarget: 'ai',
    secondary: true,
  },
  {
    id: 'hero-3',
    image:
      'https://images.unsplash.com/photo-1600718374662-0483d2b9da44?auto=format&fit=crop&w=1800&q=85',
    eyebrow: 'Confianza total',
    title: 'Filtrá, compará y que el stock hable',
    subtitle:
      'Desde SUV familiares hasta deportivos: encontrá la unidad ideal buscando por precio, año o combustible.',
    ctaLabel: 'Ver catálogo',
    ctaTarget: 'catalog',
  },
  {
    id: 'hero-4',
    image:
      'https://images.unsplash.com/photo-1494976388531-d1058494cdd8?auto=format&fit=crop&w=1800&q=85',
    eyebrow: 'Nuevo comienzo',
    title: 'Conducí sin complicaciones',
    subtitle:
      'Un e-commerce pensado para comprar tu auto de forma simple, transparente y con garantía.',
    ctaLabel: 'Empezar ahora',
    ctaTarget: 'catalog',
  },
]

const AUTOPLAY_MS = 6000

interface HeroCarouselProps {
  onNavigate: (view: AppView) => void
}

export function HeroCarousel({ onNavigate }: HeroCarouselProps) {
  const [index, setIndex] = useState(0)
  const [paused, setPaused] = useState(false)
  const timerRef = useRef<number | null>(null)

  const total = SLIDES.length

  useEffect(() => {
    if (paused) return
    timerRef.current = window.setInterval(() => {
      setIndex((current) => (current + 1) % total)
    }, AUTOPLAY_MS)
    return () => {
      if (timerRef.current !== null) window.clearInterval(timerRef.current)
    }
  }, [paused, total, index])

  const go = (next: number) => {
    setIndex(((next % total) + total) % total)
  }

  return (
    <section
      className="group relative overflow-hidden bg-slate-950"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      aria-roledescription="carrusel"
      aria-label="Destacados"
    >
      <div className="relative h-[70vh] min-h-[430px] max-h-[620px]">
        {SLIDES.map((slide, slideIndex) => (
          <div
            key={slide.id}
            className={`absolute inset-0 transition-opacity duration-700 ease-out ${
              slideIndex === index ? 'z-10 opacity-100' : 'z-0 opacity-0'
            }`}
            aria-hidden={slideIndex !== index}
          >
            <img
              src={slide.image}
              alt={slide.title}
              loading={slideIndex === 0 ? 'eager' : 'lazy'}
              className={`h-full w-full object-cover transition-transform duration-[7000ms] ease-out ${
                slideIndex === index ? 'scale-105' : 'scale-100'
              }`}
            />
            <div className="absolute inset-0 bg-gradient-to-t from-slate-950/90 via-slate-950/40 to-slate-950/10" />
            <div className="absolute inset-0 bg-gradient-to-r from-slate-950/70 via-transparent to-transparent" />
          </div>
        ))}
      </div>

      <div className="absolute inset-x-0 bottom-0">
        <div className="mx-auto flex max-w-7xl items-end justify-between gap-4 px-4 pb-10 sm:px-6 lg:px-8">
          <div key={SLIDES[index].id} className="max-w-2xl pb-2 animate-fade-in-up">
            <span className="inline-flex items-center gap-1.5 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs font-semibold uppercase tracking-widest text-white/90 backdrop-blur">
              {SLIDES[index].secondary && <Sparkles className="h-3 w-3" />}
              {SLIDES[index].eyebrow}
            </span>
            <h1 className="text-balance mt-4 text-3xl font-extrabold leading-tight tracking-tight text-white sm:text-5xl">
              {SLIDES[index].title}
            </h1>
            <p className="mt-3 hidden max-w-xl text-sm leading-relaxed text-slate-200 sm:block sm:text-base">
              {SLIDES[index].subtitle}
            </p>
            <button
              type="button"
              onClick={() => onNavigate(SLIDES[index].ctaTarget)}
              className="mt-6 inline-flex items-center gap-2 rounded-xl bg-brand-600 px-5 py-3 text-sm font-bold text-white shadow-xl shadow-brand-600/30 transition hover:bg-brand-500 hover:shadow-brand-500/40 active:scale-[0.98]"
            >
              {SLIDES[index].ctaLabel}
              <ArrowRight className="h-4 w-4" />
            </button>
          </div>

          <div className="hidden shrink-0 items-center gap-2 pb-1 lg:flex">
            <button
              type="button"
              onClick={() => go(index - 1)}
              aria-label="Slide anterior"
              className="flex h-11 w-11 items-center justify-center rounded-full border border-white/25 bg-white/10 text-white backdrop-blur transition hover:bg-white/20"
            >
              <ChevronLeft className="h-5 w-5" />
            </button>
            <button
              type="button"
              onClick={() => go(index + 1)}
              aria-label="Siguiente slide"
              className="flex h-11 w-11 items-center justify-center rounded-full border border-white/25 bg-white/10 text-white backdrop-blur transition hover:bg-white/20"
            >
              <ChevronRight className="h-5 w-5" />
            </button>
          </div>
        </div>

        <div className="flex items-center justify-center gap-2 pb-6 lg:justify-start lg:pl-[max(1rem,calc((100vw-80rem)/2+2rem))]">
          {SLIDES.map((slide, slideIndex) => (
            <button
              key={slide.id}
              type="button"
              onClick={() => go(slideIndex)}
              aria-label={`Ir al slide ${slideIndex + 1}`}
              className={`h-2 rounded-full transition-all duration-300 ${
                slideIndex === index
                  ? 'w-8 bg-brand-500'
                  : 'w-2 bg-white/35 hover:bg-white/60'
              }`}
            />
          ))}
        </div>
      </div>

      <button
        type="button"
        onClick={() => go(index - 1)}
        aria-label="Slide anterior"
        className="absolute left-3 top-1/2 z-20 hidden -translate-y-1/2 items-center justify-center rounded-full border border-white/25 bg-white/10 p-2.5 text-white opacity-0 backdrop-blur transition group-hover:opacity-100 hover:bg-white/20 md:flex lg:hidden"
      >
        <ChevronLeft className="h-5 w-5" />
      </button>
      <button
        type="button"
        onClick={() => go(index + 1)}
        aria-label="Siguiente slide"
        className="absolute right-3 top-1/2 z-20 hidden -translate-y-1/2 items-center justify-center rounded-full border border-white/25 bg-white/10 p-2.5 text-white opacity-0 backdrop-blur transition group-hover:opacity-100 hover:bg-white/20 md:flex lg:hidden"
      >
        <ChevronRight className="h-5 w-5" />
      </button>
    </section>
  )
}