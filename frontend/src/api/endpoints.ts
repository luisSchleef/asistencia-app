import { api } from './client'
import type { Asistencia, Inasistencia, TokenResponse, Usuario } from '../types/api'

export function login(correo: string, contrasena: string) {
  return api.post<TokenResponse>('/auth/login', { correo, contrasena }).then((r) => r.data)
}

export function me() {
  return api.get<Usuario>('/auth/me').then((r) => r.data)
}

export function marcarEntrada() {
  return api.post<Asistencia>('/asistencias/entrada').then((r) => r.data)
}

export function marcarSalida() {
  return api.post<Asistencia>('/asistencias/salida').then((r) => r.data)
}

export function listarAsistencias() {
  return api.get<Asistencia[]>('/asistencias').then((r) => r.data)
}

export function reporteAtrasos() {
  return api.get<Asistencia[]>('/reportes/atrasos').then((r) => r.data)
}

export function reporteSalidasAnticipadas() {
  return api.get<Asistencia[]>('/reportes/salidas-anticipadas').then((r) => r.data)
}

export function reporteInasistencias() {
  return api.get<Inasistencia[]>('/reportes/inasistencias').then((r) => r.data)
}

export interface UsuarioInput {
  nombre: string
  correo: string
  contrasena: string
  rol: 'ADMIN' | 'EMPLEADO'
}

export function listarUsuarios() {
  return api.get<Usuario[]>('/usuarios').then((r) => r.data)
}

export function crearUsuario(input: UsuarioInput) {
  return api.post<Usuario>('/usuarios', input).then((r) => r.data)
}

export function actualizarUsuario(id: number, input: UsuarioInput) {
  return api.put<Usuario>(`/usuarios/${id}`, input).then((r) => r.data)
}

export function eliminarUsuario(id: number) {
  return api.delete(`/usuarios/${id}`)
}
