import { create } from 'zustand'

export interface Toast {
  id: number
  tipo: 'ok' | 'error'
  mensaje: string
}

interface EstadoToasts {
  toasts: Toast[]
  notificar: (tipo: Toast['tipo'], mensaje: string) => void
  quitar: (id: number) => void
}

let siguienteId = 1
const DURACION_MS = 4200

export const useToasts = create<EstadoToasts>((set) => ({
  toasts: [],
  notificar: (tipo, mensaje) => {
    const id = siguienteId++
    set((s) => ({ toasts: [...s.toasts, { id, tipo, mensaje }] }))
    setTimeout(() => {
      set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }))
    }, DURACION_MS)
  },
  quitar: (id) => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
}))
