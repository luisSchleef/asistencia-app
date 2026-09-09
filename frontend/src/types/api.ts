export type Rol = 'ADMIN' | 'EMPLEADO'

export interface Usuario {
  id: number
  nombre: string
  correo: string
  rol: Rol
}

export interface TokenResponse {
  accessToken: string
  tokenType: string
}

export interface Asistencia {
  id: number
  usuarioId: number
  usuarioNombre: string
  tipo: 'ENTRADA' | 'SALIDA'
  fecha: string
  hora: string
}

export interface Inasistencia {
  usuarioId: number
  usuarioNombre: string
  fecha: string
}

export interface ErrorResponse {
  status: number
  error: string
  mensaje?: string
  campos?: Record<string, string>
}
