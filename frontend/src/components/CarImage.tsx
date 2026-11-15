import { useEffect, useState } from 'react'
import { ImageOff } from 'lucide-react'

type Status = 'loading' | 'loaded' | 'error'

interface CarImageProps {
  src: string
  alt: string
  /** Clases del contenedor (tamaño, radio, etc.). Siempre es `relative`. */
  className?: string
  /** Clases extra para la etiqueta <img> (p. ej. zoom al hover). */
  imgClassName?: string
  eager?: boolean
  /** Variante chica (miniaturas): solo ícono, sin texto. */
  compact?: boolean
}

/**
 * Imagen de vehículo con esqueleto mientras carga y un reemplazo explícito si
 * la URL falla. Antes, una URL rota dejaba el shimmer girando para siempre y la
 * tarjeta parecía "cargando" sin terminar nunca.
 */
export function CarImage({
  src,
  alt,
  className = '',
  imgClassName = '',
  eager = false,
  compact = false,
}: CarImageProps) {
  const [status, setStatus] = useState<Status>(src ? 'loading' : 'error')

  useEffect(() => {
    setStatus(src ? 'loading' : 'error')
  }, [src])

  return (
    <div className={`relative overflow-hidden bg-slate-100 dark:bg-slate-800 ${className}`}>
      {status === 'loading' && <div className="absolute inset-0 skeleton" aria-hidden />}

      {status === 'error' ? (
        <div
          role="img"
          aria-label={alt}
          className="flex h-full w-full flex-col items-center justify-center gap-1.5 text-slate-400 dark:text-slate-500"
        >
          <ImageOff className={compact ? 'h-5 w-5' : 'h-8 w-8'} aria-hidden />
          {!compact && <span className="text-[11px] font-medium">Imagen no disponible</span>}
        </div>
      ) : (
        <img
          src={src}
          alt={alt}
          loading={eager ? 'eager' : 'lazy'}
          decoding="async"
          onLoad={() => setStatus('loaded')}
          onError={() => setStatus('error')}
          className={`h-full w-full object-cover transition-opacity duration-500 ${
            status === 'loaded' ? 'opacity-100' : 'opacity-0'
          } ${imgClassName}`}
        />
      )}
    </div>
  )
}
