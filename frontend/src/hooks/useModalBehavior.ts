import { useEffect, useRef } from 'react'

let openModals = 0
let previousBodyOverflow = ''

/**
 * Comportamiento común de los diálogos modales:
 * - bloquea el scroll de la página mientras el diálogo está abierto (la página
 *   de fondo no se desplaza "por debajo" del modal);
 * - cierra con Escape;
 * - lleva el foco al panel al abrir y lo devuelve al disparador al cerrar.
 *
 * Devuelve la ref que hay que asignar al panel (con `tabIndex={-1}`).
 */
export function useModalBehavior<T extends HTMLElement>(onClose: () => void) {
  const panelRef = useRef<T | null>(null)

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  useEffect(() => {
    if (openModals === 0) {
      previousBodyOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
    }
    openModals += 1

    const trigger = document.activeElement as HTMLElement | null
    panelRef.current?.focus({ preventScroll: true })

    return () => {
      openModals -= 1
      if (openModals === 0) {
        document.body.style.overflow = previousBodyOverflow
      }
      if (trigger && typeof trigger.focus === 'function' && document.contains(trigger)) {
        trigger.focus({ preventScroll: true })
      }
    }
  }, [])

  return panelRef
}
