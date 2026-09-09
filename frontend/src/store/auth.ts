import { create } from 'zustand'
import type { Usuario } from '../types/api'

interface EstadoSesion {
  token: string | null
  usuario: Usuario | null
  login: (token: string, usuario: Usuario) => void
  logout: () => void
}

const STORAGE_KEY = 'asistencias.sesion'

function cargar(): { token: string | null; usuario: Usuario | null } {
  try {
    const crudo = localStorage.getItem(STORAGE_KEY)
    return crudo ? JSON.parse(crudo) : { token: null, usuario: null }
  } catch {
    return { token: null, usuario: null }
  }
}

const inicial = cargar()

export const useSesion = create<EstadoSesion>((set) => ({
  token: inicial.token,
  usuario: inicial.usuario,
  login: (token, usuario) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, usuario }))
    set({ token, usuario })
  },
  logout: () => {
    localStorage.removeItem(STORAGE_KEY)
    set({ token: null, usuario: null })
  },
}))
