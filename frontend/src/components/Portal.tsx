import type { ReactNode } from 'react'
import { createPortal } from 'react-dom'

/**
 * Renderiza los hijos directamente bajo <body>.
 *
 * Los diálogos usan `position: fixed`; si quedan dentro de un ancestro con
 * `transform`, `filter` o `will-change` (p. ej. una tarjeta con hover que se
 * eleva, o un bloque con animación de aparición), ese ancestro pasa a ser su
 * contenedor y el diálogo se dibuja recortado dentro de la tarjeta. El portal
 * evita esa dependencia del árbol visual.
 */
export function Portal({ children }: { children: ReactNode }) {
  if (typeof document === 'undefined') return null
  return createPortal(children, document.body)
}
