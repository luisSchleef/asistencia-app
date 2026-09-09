import { create } from 'zustand'

export type TipoMarca = 'ENTRADA' | 'SALIDA'

interface EstadoMarcas {
  usuarioId: number | null
  fecha: string
  entrada: string | null
  salida: string | null
  bloqueoHasta: number
  registrar: (tipo: TipoMarca, hora: string) => void
  bloquear: (milisegundos: number) => void
  desbloquear: () => void
  sincronizar: (usuarioId: number) => void
}

const STORAGE_KEY = 'asistencias.marcas'

function fechaLocal(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function cargar(): { usuarioId: number | null; fecha: string; entrada: string | null; salida: string | null; bloqueoHasta: number } {
  try {
    const crudo = localStorage.getItem(STORAGE_KEY)
    if (crudo) {
      const datos = JSON.parse(crudo)

      if (datos.fecha === fechaLocal()) return datos
    }
  } catch {

  }
  return { usuarioId: null, fecha: fechaLocal(), entrada: null, salida: null, bloqueoHasta: 0 }
}

function guardar(estado: {
  usuarioId: number | null
  fecha: string
  entrada: string | null
  salida: string | null
  bloqueoHasta: number
}) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(estado))
}

const inicial = cargar()

function vacio(usuarioId: number) {
  return { usuarioId, fecha: fechaLocal(), entrada: null, salida: null, bloqueoHasta: 0 }
}

export const useMarcas = create<EstadoMarcas>((set) => ({
  ...inicial,
  registrar: (tipo, hora) =>
    set((s) => {
      const estado = { ...s }
      if (tipo === 'ENTRADA') estado.entrada = hora
      else estado.salida = hora
      guardar(estado)
      return estado
    }),
  bloquear: (milisegundos) =>
    set((s) => {
      const estado = { ...s, bloqueoHasta: Date.now() + milisegundos }
      guardar(estado)
      return estado
    }),
  desbloquear: () =>
    set((s) => {
      const estado = { ...s, bloqueoHasta: 0 }
      guardar(estado)
      return estado
    }),

  sincronizar: (usuarioId) =>
    set((s) => {
      const hoy = fechaLocal()
      if (s.usuarioId === usuarioId && s.fecha === hoy) return s
      const estado = vacio(usuarioId)
      guardar(estado)
      return estado
    }),
}))
