import { useEffect } from 'react'
import type { ReactNode } from 'react'
import Icon from './Icon'

export default function Modal({ titulo, onCerrar, children }: {
  titulo: string
  onCerrar: () => void
  children: ReactNode
}) {
  useEffect(() => {
    function alTeclear(e: KeyboardEvent) {
      if (e.key === 'Escape') onCerrar()
    }
    document.addEventListener('keydown', alTeclear)
    return () => document.removeEventListener('keydown', alTeclear)
  }, [onCerrar])

  return (
    <div className="modal-overlay" onClick={onCerrar}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-cabecera">
          <h2>{titulo}</h2>
          <button className="icono-boton" onClick={onCerrar} aria-label="Cerrar">
            <Icon nombre="equis" tamaño={16} />
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
