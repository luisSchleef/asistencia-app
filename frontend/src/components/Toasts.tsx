import { useToasts } from '../store/toast'
import Icon from './Icon'

export default function Toasts() {
  const toasts = useToasts((s) => s.toasts)
  const quitar = useToasts((s) => s.quitar)

  if (toasts.length === 0) return null

  return (
    <div className="toasts" aria-live="polite">
      {toasts.map((t) => (
        <div key={t.id} className={`toast ${t.tipo}`} role={t.tipo === 'error' ? 'alert' : 'status'}>
          <Icon nombre={t.tipo === 'ok' ? 'check' : 'alerta'} tamaño={18} />
          <p>{t.mensaje}</p>
          <button className="icono-boton" onClick={() => quitar(t.id)} aria-label="Cerrar notificación">
            <Icon nombre="equis" tamaño={14} />
          </button>
        </div>
      ))}
    </div>
  )
}
