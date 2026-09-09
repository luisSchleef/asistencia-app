import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import * as endpoints from '../api/endpoints'
import Icon from '../components/Icon'
import { useSesion } from '../store/auth'
import type { ErrorResponse, Usuario } from '../types/api'

export default function LoginPage() {
  const navegar = useNavigate()
  const loginSesion = useSesion((s) => s.login)
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setCargando(true)
    try {
      const { accessToken } = await endpoints.login(correo.trim(), contrasena)

      const usuario = await api.get<Usuario>('/auth/me', {
        headers: { Authorization: `Bearer ${accessToken}` },
      }).then((r) => r.data)
      loginSesion(accessToken, usuario)
      navegar('/', { replace: true })
    } catch (err) {
      const respuesta = (err as { response?: { data?: ErrorResponse } }).response?.data
      setError(respuesta?.mensaje ?? respuesta?.error ?? 'No se pudo iniciar sesión')
    } finally {
      setCargando(false)
    }
  }

  return (
    <main className="centro fondo-login">
      <form className="tarjeta login" onSubmit={enviar}>
        <div className="login-marca">
          <span className="login-logo">
            <Icon nombre="reloj" tamaño={26} />
          </span>
          <h1>Control de Asistencia</h1>
          <p className="subtitulo">Ingresa con tu correo corporativo</p>
        </div>
        <label>
          Correo
          <input
            type="email"
            placeholder="correo@empresa.cl"
            autoComplete="username"
            autoFocus
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
            required
          />
        </label>
        <label>
          Contraseña
          <input
            type="password"
            placeholder="••••••••"
            autoComplete="current-password"
            value={contrasena}
            onChange={(e) => setContrasena(e.target.value)}
            required
          />
        </label>
        {error && (
          <p className="error" role="alert">
            <Icon nombre="alerta" tamaño={16} />
            {error}
          </p>
        )}
        <button type="submit" className="primario" disabled={cargando}>
          {cargando && <span className="spinner" aria-hidden="true" />}
          {cargando ? 'Ingresando...' : 'Ingresar'}
        </button>
      </form>
    </main>
  )
}
